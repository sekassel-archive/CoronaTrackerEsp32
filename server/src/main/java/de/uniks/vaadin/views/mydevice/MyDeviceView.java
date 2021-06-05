package de.uniks.vaadin.views.mydevice;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.uniks.vaadin.views.main.MainView;

@Route(value = "myDeviceVerification", layout = MainView.class)
@PageTitle("My ESP32 Device")
//@CssImport("./styles/views/mydevice/my-device-view.css")
public class MyDeviceView extends HorizontalLayout {

    public MyDeviceView() {
        setId("my-device-view");

        //add();
    }
}
