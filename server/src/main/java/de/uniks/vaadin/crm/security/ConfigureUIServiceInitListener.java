package de.uniks.vaadin.crm.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.uniks.vaadin.views.login.LoginView;
import de.uniks.vaadin.views.mydevice.MyDeviceView;
import org.springframework.stereotype.Component;

@Component
public class ConfigureUIServiceInitListener implements VaadinServiceInitListener {
    // listen for the initialization of the UI (the internal root component in Vaadin) and
    // then add a listener before every view transition
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            final UI ui = uiEvent.getUI();
            ui.addBeforeEnterListener(this::authenticateNavigation);
        });
    }

    // in authenticateNavigation, reroute all requests to the login, if the user is not logged in
    private void authenticateNavigation(BeforeEnterEvent event) {
        if (!MyDeviceView.class.equals(event.getNavigationTarget())) {
            // so login should only for verification form be forced
            // TODO: maybe change to better solution
            return;
        }
        if (!LoginView.class.equals(event.getNavigationTarget())
                && !SecurityUtils.isUserLoggedIn()) {
            event.rerouteTo(LoginView.class);
        }
    }
}
