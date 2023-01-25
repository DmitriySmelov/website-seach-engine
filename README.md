  <div align="center">
<h1><b>Поисковый движок</b></h1>
<p>Поисковый движок предназначен для индексации сайтов, с последующей возможность поиска по проиндексированным сайтам.</p>
<p>Программа представляет из себя spring boot приложение работающее в многопоточной среде, 
и использующее для хранения данных sql базу данных (Mysql).</p>
<h2><b>Перед запуском.</b></h2>
<p>Установите на свой компьютер MySQL-сервер, если он ещё не установлен,
и создайте в нём пустую базу данных <b>search_engine</b>.
В конфигурационном файле (application.yaml) укажите пароль и имя пользователя для базы данных.</p>
<br/>
<p class="fig"><img src="description\image\img.png" 
    alt="Изображение"></p>
<br/>
<p>В этом же файле задайте адреса и имена сайтов, по которым будет проводиться индексация.</p>
<br/>
<p class="fig"><img src="description\image\img_3.png" 
    alt="Изображение"></p>
<br/>
<p>После первого запуска вся структура базы данных подтянется из скрипта.</p>
<p>После первого запуска можете удалить скрипт. И изменить параметр в файле application.yaml с 
always на never</p>

<p class="fig"><img src="description\image\img_10.png" 
    alt="Изображение"></p>
<br/>
<p>Для лемматизации слов в проекте применяется LuceneMorphology, зависимости подгружаются из репозитория
разработанного Skillbox.</p>
<p>Вам необходимо указать токен для доступа к данному Maven-репозиторию. 
Для указания токена найдите или создайте файл settings.xml.</p>
<br/>
<p> В Windows он располагается в директории: C:/Users/&lt;Имя вашего пользователя&gt;/.m2</p>
<p> В Linux — в директории: /home/&lt;Имя вашего пользователя&gt;/.m2</p>
<p> В macOs — по адресу: /Users/&lt;Имя вашего пользователя&gt;/.m2</p>
<br/>
<p>Если файла settings.xml нет, создайте его и вставьте в него код из файла settings.xml находящегося в проете, 
в папке description.</p>
<p>Если файл у вас уже есть, но в нём нет блока &lt;servers&gt;, то добавьте в него только этот блок. 
Если этот блок в файле есть, добавьте внутрь него блок &lt;server&gt; кода из файла settings.xml находящегося в проете, 
в папке description.</p>
<br/>
<p>В блоке &lt;value&gt; находится уникальный токен доступа. Если у вас возникнет «401 Ошибка Авторизации» 
при попытке получения зависимостей, возьмите актуальный токен доступа из документа по
<a href="https://docs.google.com/document/d/1rb0ysFBLQltgLTvmh-ebaZfJSI7VwlFlEYT9V5_aPjc/edit">ссылке</a>.</p>
<p>Обязательно почистите кэш maven. Самый надёжный способ — удалить директорию:</p>
<br/>
<p> В Windows: C:/Users/&lt;Имя вашего пользователя&gt;/.m2/repository</p>
<p> В Linux: /home/&lt;Имя вашего пользователя&gt;/.m2/repository</p>
<p> В macOs: /Users/&lt;Имя вашего пользователя&gt;/.m2/repository</p>
<br/>
<p>После этого снова попробуйте обновить данные из pom.xml.</p>

<h2><b>Структура API.</b></h2>
<p><b>Запуск полной индексации — метод GET </b></p>
<p><b>Запрос пользователя: /api/startIndexing
</b></p>
<br/>
<p>Метод запускает полную индексацию всех сайтов или полную переиндексацию, если они уже проиндексированы.
Если в настоящий момент индексация или переиндексация уже запущена, метод возвращает соответствующее сообщение об ошибке. 
</p>

<br/>
<p><b>Формат ответа в случае успеха:</b></p>
<p class="fig"><img src="description\image\img_2.png" 
    alt="Изображение"></p>
<br/>
<p><b>Формат ответа в случае ошибки:</b></p>
<p class="fig"><img src="description\image\img_6.png" 
    alt="Изображение"></p>
<br/>

<p><b>Остановка текущей индексации — метод GET </b></p>
<p><b>Запрос пользователя: /api/stopIndexing</b></p>
<br/>
<p>Метод останавливает текущий процесс индексации (переиндексации). Если в настоящий момент индексация 
или переиндексация не происходит, метод возвращает соответствующее сообщение об ошибке. 
</p>
<br/>
<p><b>Формат ответа в случае успеха:</b></p>
<p class="fig"><img src="description\image\img_2.png" 
    alt="Изображение"></p>
<br/>
<p><b>Формат ответа в случае ошибки:</b></p>
<p class="fig"><img src="description\image\img_5.png" 
    alt="Изображение"></p>
<br/>

<p><b>Добавление или обновление отдельной страницы — метод POST </b></p>
<p><b>Запрос пользователя: /api/indexPage</b></p>
<br/>
<p>Метод добавляет в индекс или обновляет отдельную страницу, адрес которой передан в параметре.
Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.
</p>
<p><b>Параметры:</b></p>
<p> url — адрес страницы, которую нужно переиндексировать.</p>
<br/>
<p><b>Формат ответа в случае успеха:</b></p>
<p class="fig"><img src="description\image\img_2.png" 
    alt="Изображение"></p>
<br/>
<p><b>Формат ответа в случае ошибки:</b></p>
<p class="fig"><img src="description\image\img_7.png" 
    alt="Изображение"></p>
<br/>

<p><b>Статистика — метод GET </b></p>
<p><b>Запрос пользователя: /api/statistics</b></p>
<p>Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.
</p>
<br/>
<p><b>Формат ответа:</b></p>
<p class="fig"><img src="description\image\img_8.png" 
    alt="Изображение"></p>
<br/>

<p><b>Получение данных по поисковому запросу — метод GET</b></p>
<p><b>Запрос пользователя: /api/search</b></p>
<p>Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
Чтобы выводить результаты порционно, задается параметры offset (сдвиг от начала списка результатов) 
и limit (количество результатов, которое необходимо вывести).
В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit, 
и массив data с результатами поиска.
Если ещё нет готового индекса(сайт, по которому ищем, или все сайты сразу не проиндексированы), 
метод  вернет соответствующую ошибку.
</p>
<p><b>Параметры:</b></p>

<p> query — поисковый запрос;</p>
<p> site — сайт, по которому осуществлять поиск, задаётся в формате адреса, например: http://www.site.com.</p>
<p>Параметр сайт может быть не задан, тогда поиск производится по всем проиндексированным сайтам.</p>
<p> offset — сдвиг от 0 для постраничного вывода;</p>
<p> limit — количество результатов, которое необходимо вывести.</p>
<br/>
<p><b>Формат ответа в случае успеха:</b></p>
<p class="fig"><img src="description\image\img_9.png" 
    alt="Изображение"></p>
<br/>
<p><b>Формат ответа в случае ошибки:</b></p>
<p class="fig"><img src="description\image\img_7.png" 
    alt="Изображение"></p>
<br/>

</div>