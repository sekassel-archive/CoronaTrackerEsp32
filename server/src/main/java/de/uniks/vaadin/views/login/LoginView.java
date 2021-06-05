package de.uniks.vaadin.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfiguration;
import de.uniks.vaadin.views.main.MainView;

import javax.servlet.annotation.WebServlet;

@Route(value = "verificationProcess", layout = MainView.class)
@PageTitle("Login")
public class LoginView extends HorizontalLayout {

    public LoginView() {
        setId("login-view");

        LoginForm login = new LoginForm();
        login.setI18n(createCustomLoginI18n());

        add(login);
    }

    private LoginI18n createCustomLoginI18n() {
        final LoginI18n i18n = LoginI18n.createDefault();

        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("UUID");
        i18n.getHeader().setDescription("32 character long UUID");
        i18n.getForm().setUsername("UUID");
        i18n.getForm().setTitle("Verification");
        i18n.getForm().setSubmit("OK");
        i18n.getForm().setPassword("PIN");
        i18n.getForm().setForgotPassword(".");
        i18n.getErrorMessage().setTitle("Ups");
        i18n.getErrorMessage().setMessage("Something went wrong.");
        i18n.setAdditionalInformation("The UUID have to be 32 digits long and PIN shouldn't be older than 15 minutes!");
        return i18n;
    }
}
