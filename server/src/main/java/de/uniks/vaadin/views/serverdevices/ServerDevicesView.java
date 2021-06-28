package de.uniks.vaadin.views.serverdevices;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.uniks.cwa.CwaDataInterpreter;
import de.uniks.postgres.db.utils.UserPostgreSql;
import de.uniks.vaadin.views.main.MainView;
import de.uniks.vaadin.views.viewmodels.RsinEntrys;

import java.util.ArrayList;
import java.util.List;

@Route(value = "serverDevicesInformation", layout = MainView.class)
@PageTitle("Server Devices")
@CssImport("./styles/views/serverdevices/server-devices-view.css")
public class ServerDevicesView extends HorizontalLayout {
    private static UserPostgreSql userPostgreSql = new UserPostgreSql();

    public ServerDevicesView() {
        setId("server-devices-view");

        TextField trackerUserField = new TextField();
        trackerUserField.setValue(userPostgreSql.getUserCount().toString());
        trackerUserField.setLabel("ESP32 Tracker Users");
        trackerUserField.setReadOnly(true);

        TextField infectionCheckField = new TextField();
        infectionCheckField.setValue(CwaDataInterpreter.lastCheckTimeString);
        infectionCheckField.setLabel("Last Infection Check Status");
        infectionCheckField.setWidth("400px");
        infectionCheckField.setReadOnly(true);

        Details component = new Details();
        component.setSummaryText("Show Uni Tracker collected Information");
        component.addContent(trackerUserField);
        component.addContent(infectionCheckField);
        component.addThemeVariants(DetailsVariant.REVERSE, DetailsVariant.FILLED);
        component.setOpened(true);
        add(component);

        List<RsinEntrys> localInfectionList = new ArrayList<>();
        CwaDataInterpreter.getInfectedUserList().forEach(infectedUser -> {
            String rsin = infectedUser.getRsin().toString();
            List<byte[]> tekList = List.of(infectedUser.getTek());
            localInfectionList.add(new RsinEntrys(rsin, tekList));
        });

        if (localInfectionList.isEmpty()) {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(true);

            Details idleComp = new Details();
            idleComp.setSummaryText("No Infections detected. If there are infections detected, they will be shown here.");
            idleComp.addContent(progressBar);
            idleComp.setEnabled(false);
            idleComp.setOpened(true);
            idleComp.addThemeVariants(DetailsVariant.REVERSE, DetailsVariant.FILLED);

            add(idleComp);
        } else {
            TreeGrid<RsinEntrys> grid = new TreeGrid<>();
            grid.setItems(localInfectionList);

            grid.addColumn(RsinEntrys::getRsinDate).setHeader("Date").setSortable(true);
            grid.addColumn(RsinEntrys::getRsin).setHeader("RSIN");
            grid.addColumn(RsinEntrys::getTekEntrys).setHeader("TEK Entrys");
            grid.setHeightByRows(true);
            grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,
                    GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_ROW_STRIPES);
            grid.setSelectionMode(Grid.SelectionMode.NONE);
            grid.setItemDetailsRenderer(TemplateRenderer.<RsinEntrys>of(
                    "<div style='border: 1px solid gray; padding: 5px; width: 90%; box-sizing: border-box;'>"
                            + "<div><b>TEK Data: </b>[[item.tekAsBlock]]</div>"
                            + "</div>")
                    .withProperty("tekAsBlock", RsinEntrys::getTekListAsBlock)
                    .withEventHandler("handleClick", rsin -> {
                        grid.getDataProvider().refreshItem(rsin);
                    }));
            add(grid);
        }
    }
}
