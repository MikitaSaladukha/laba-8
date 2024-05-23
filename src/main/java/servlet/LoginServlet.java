package servlet;

import entity.ChatUser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

@WebServlet(name="LoginServlet",urlPatterns = {"/login.do"})
public class LoginServlet extends ChatServlet {
    private static final long serialVersionUID = 1L;
    // Длительность сессии, в секундах
    private int sessionTimeout = 10*60;
    int count;
    public void init() throws ServletException {
        super.init();
        // Прочитать из конфигурации значение параметра SESSION_TIMEOUT
        String value = getServletConfig().getInitParameter("SESSION_TIMEOUT");
        // Если он задан, переопределить длительность сессии по умолчанию
        if (value!=null) {
            sessionTimeout = Integer.parseInt(value);
        }
    }
    // Метод будет вызван при обращении к сервлету HTTP-методом GET
    // т.е. когда пользователь просто открывает адрес в браузере
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Проверить, есть ли уже в сессии заданное имя пользователя?
        String name = (String)request.getSession().getAttribute("name");
        // Извлечь из сессии сведения о предыдущей ошибке (возможной)
       String errorMessage = (String)request.getSession().getAttribute("error");
        // Идентификатор предыдущей сессии изначально пуст
        String previousSessionId = null;
        // Если в сессии имя не сохранено, то попытаться
// восстановить имя через cookie
        if (name==null) {
            try {
                // Найти cookie с именем sessionId
                for (Cookie aCookie : request.getCookies()) {
                    if (aCookie.getName().equals("sessionId")) {
                        previousSessionId = aCookie.getValue();
                        break;
                    }
                }
            }catch (NullPointerException e){
                name = null;
            }
            if (previousSessionId!=null) {
                // Мы нашли session cookie
// Попытаться найти пользователя с таким sessionId
                for (ChatUser aUser: activeUsers.values()) {
                    if
                    (aUser.getSessionId().equals(previousSessionId)) {
                        name = aUser.getName();
                        aUser.setSessionId(request.getSession().getId());
                    }
                }
            }
        }
// Если в сессии имеется не пустое имя пользователя, то...
        if (name!=null && !"".equals(name)) {
            errorMessage = processLogonAttempt(name, request, response);
        }
// Пользователю необходимо ввести имя. Показать форму
// Задать кодировку HTTP-ответа
        response.setCharacterEncoding("utf8");
        // Получить поток вывода для HTTP-ответа
        PrintWriter pw = response.getWriter();
        pw.println("<html><head><title>Мега-чат!</title><meta httpequiv='Content-Type' content='text/html; charset=utf-8'/></head>");
        // Если возникла ошибка - сообщить о ней
        if (errorMessage!=null) {
            pw.println("<p><font color='red'>" + errorMessage + "</font></p>");
        }
        // Вывести форму
        pw.println("<form action='/lab8/' method='post'>Enter the name: <input type='text' name='name' value=''><input type='submit' value='Connect to the chat'>");
        pw.println("</form></body></html>");
        // Сбросить сообщение об ошибке в сессии
        request.getSession().setAttribute("error", null);
    }
    // Метод будет вызван при обращении к сервлету HTTP-методом POST
// т.е. когда пользователь отправляет сервлету данные
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Задать кодировку HTTP-запроса - очень важно!
// Иначе вместо символов будет абракадабра
        request.setCharacterEncoding("UTF-8");
        // Извлечь из HTTP-запроса значение параметра 'name'
        String name = (String)request.getParameter("name");
        String errorMessage = null;
        if (name==null || "".equals(name)) {
            errorMessage = "Name cannot be empty!";
        }  else  if(count == 3) {
            errorMessage = "number of users exceeded!";
        }  else {
            errorMessage = processLogonAttempt(name, request, response);
        }
        if (errorMessage!=null) {
            request.getSession().setAttribute("name", null);
            request.getSession().setAttribute("error", errorMessage);
            response.sendRedirect(response.encodeRedirectURL("/lab8/"));
        }
    }
    // Возвращает текстовое описание возникшей ошибки или null
    String processLogonAttempt(String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionId = request.getSession().getId();
        ChatUser aUser = activeUsers.get(name);
        if (aUser==null) {
            aUser = new ChatUser(name, Calendar.getInstance().getTimeInMillis(), sessionId);
            synchronized (activeUsers) {
                activeUsers.put(aUser.getName(), aUser);
            }
        }
        count ++;



        if (aUser.getSessionId().equals(sessionId) || aUser.getLastInteractionTime()<(Calendar.getInstance().getTimeInMillis()- sessionTimeout*1000)) {
            // Если указанное имя принадлежит текущему пользователю,
// либо оно принадлежало кому-то другому, но сессия истекла,
// то одобрить запрос пользователя на это имя
// Обновить имя пользователя в сессии
            request.getSession().setAttribute("name", name);
            aUser.setLastInteractionTime(Calendar.getInstance().getTimeInMillis());
            Cookie sessionIdCookie = new Cookie("sessionId", sessionId);
            sessionIdCookie.setMaxAge(60*60*24*365);
            response.addCookie(sessionIdCookie);
            response.sendRedirect(response.encodeRedirectURL("/lab8/view.htm"));
            return null;
        } else {
            return "Sorry, <strong>" + name + "</strong> is engaged. Please try another one!";
        }
    }
}
