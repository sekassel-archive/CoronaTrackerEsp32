package de.uniks.vaadin.views.login;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.uniks.vaadin.views.main.MainView;

@Route(value = "verificationProcess", layout = MainView.class)
@PageTitle("Login | Vaadin CRM")
public class LoginView extends HorizontalLayout implements BeforeEnterObserver {

    private LoginForm login = new LoginForm();

    public LoginView() {
        setId("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        login.setI18n(createCustomLoginI18n());
        login.setAction("verificationProcess");
        add(login);
    }

    private LoginI18n createCustomLoginI18n() {
        final LoginI18n i18n = LoginI18n.createDefault();

        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("UUID");
        i18n.getHeader().setDescription("36 character long UUID");
        i18n.getForm().setUsername("UUID");
        i18n.getForm().setTitle("Verification");
        i18n.getForm().setSubmit("OK");
        i18n.getForm().setPassword("PIN");
        i18n.getForm().setForgotPassword("");
        i18n.getErrorMessage().setTitle("Ups");
        i18n.getErrorMessage().setMessage("Something went wrong.");
        i18n.setAdditionalInformation("The UUID have to be 36 digits long and PIN shouldn't be older than 10 minutes!");
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // inform the user about an authentication error
        if(beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
