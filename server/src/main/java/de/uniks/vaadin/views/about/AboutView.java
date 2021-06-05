package de.uniks.vaadin.views.about;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.uniks.vaadin.views.main.MainView;

@Route(value = "about", layout = MainView.class)
@CssImport("./styles/views/about/about-view.css")
@PageTitle("About")
@RouteAlias(value = "", layout = MainView.class)
public class AboutView extends Div {

    public AboutView() {
        setId("about-view");
        Anchor anchor = new Anchor("https://github.com/sekassel/CoronaTrackerEsp32", "ESP32 Based Corona Tracker");
        add(anchor);
    }

}
