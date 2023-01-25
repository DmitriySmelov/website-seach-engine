package searchengine.dto.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import searchengine.model.Page;

import java.util.List;

@Getter
@NoArgsConstructor
public class PageInfo
{
    private Page page;
    private List<String> childLinks;

    public PageInfo(Page page, List<String> childLinks)
    {
        this.page = page;
        this.childLinks = childLinks;
    }
}
