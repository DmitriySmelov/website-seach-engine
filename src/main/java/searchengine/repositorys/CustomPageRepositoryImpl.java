package searchengine.repositorys;

import org.springframework.http.HttpStatus;
import searchengine.dto.statistics.SearchPageInfo;
import searchengine.exceptions.SearchException;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CustomPageRepositoryImpl implements CustomPageRepository {

    String userErrorMessage = "Ошибка при попытки произвести поиск.";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SearchPageInfo> getSearchPageInfoByLemmas(List<String> lemmas, int limit, int offset, Integer siteId) {
        if (lemmas.size() == 0) return new ArrayList<>();
        StringBuilder query = new StringBuilder();
        boolean isSearchByAllSites = siteId == null;

        ArrayList<Object> parameters = new ArrayList<>(lemmas);
        Integer countPageId = lemmas.size();
        if (!isSearchByAllSites) parameters.add(siteId);
        parameters.add(countPageId);
        parameters.add(limit);
        parameters.add(offset);
        int parametersCount = lemmas.size();

        query.append("with search as (select indexes.page_id as id, sum(grade) as grade from indexes " +
                "join lemmas as lem on lemma_id = lem.id " +
                "where lem.lemma in ");
        appendLemmaParameterNumbers(query, lemmas.size());
        if (!isSearchByAllSites) query.append(String.format(" and lem.site_id = ?%d", ++parametersCount));
        query.append(String.format(" group by page_id " +
                        "having count(page_id) = ?%d) " +
                        "select search.id, (grade / (select max(grade) from search)) as relevance, " +
                        "(select count(id) from search) " +
                        "from search " +
                        "order by relevance desc limit ?%d offset ?%d", ++parametersCount,
                ++parametersCount, ++parametersCount));

        Query query1 = entityManager.createNativeQuery(String.valueOf(query));

        for (int i = 0; i < parameters.size(); i++) {
            query1.setParameter(i + 1, parameters.get(i));
        }
        List<Object[]> list = query1.getResultList();
        List<SearchPageInfo> info = transformResultToSearchPageInfo(list);
        return info;
    }

    private void appendLemmaParameterNumbers(StringBuilder query, int lemmasCount) {
        query.append("(");
        for (int i = 1; i <= lemmasCount; i++) {
            query.append("?").append(i);
            if (i != lemmasCount) query.append(",");
        }
        query.append(")");
    }

    private List<SearchPageInfo> transformResultToSearchPageInfo(List<Object[]> resultList) {
        List<SearchPageInfo> pages = new ArrayList<>();

        Class clazz = SearchPageInfo.class;
        Field[] fields = clazz.getDeclaredFields();

        try {
            List<Method> setters = getSettersOrderByClassFields(fields, clazz);
            for (Object[] queryResult : resultList) {
                SearchPageInfo info = new SearchPageInfo();

                for (int i = 0; i < setters.size(); i++) {
                    setters.get(i).invoke(info, castObject(queryResult[i], fields[i].getType()));
                }
                pages.add(info);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new SearchException(userErrorMessage, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return pages;
    }

    private List<Method> getSettersOrderByClassFields(Field[] fields, Class clazz) throws NoSuchMethodException {
        List<Method> setters = new ArrayList<>();
        for (Field field : fields) {
            String fieldName = field.getName();
            String fieldNameForSetter = fieldName.substring(0, 1).toUpperCase().concat(fieldName.substring(1));

            Method setter = clazz.getDeclaredMethod("set" + fieldNameForSetter, field.getType());
            setters.add(setter);

        }
        return setters;
    }

    private <T> T castObject(Object obj, Class<T> clazz) {
        return clazz.cast(obj);
    }
}
