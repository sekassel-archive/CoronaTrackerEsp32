package de.uniks.vaadin.views.serverdevices;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.uniks.vaadin.views.main.MainView;

@Route(value = "serverDevicesInformation", layout = MainView.class)
@PageTitle("Server Devices")
//@CssImport("./styles/views/mydevice/server-devices-view.css")
//@RouteAlias(value = "", layout = MainView.class)
public class ServerDevicesView extends HorizontalLayout {
    public ServerDevicesView() {
        setId("server-devices-view");


        //add();
    }
}
