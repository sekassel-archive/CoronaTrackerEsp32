package de.uniks.views.rsinoverview;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import de.uniks.SQLite.SQLite;
import de.uniks.views.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "rsin", layout = MainView.class)
@PageTitle("RSIN Overview")
@CssImport("./styles/views/rsinoverview/rsin-overview-view.css")
@RouteAlias(value = "", layout = MainView.class)
public class RsinOverviewView extends HorizontalLayout {

    private TextField name;
    private Button sayHello;

    public RsinOverviewView() {
        setId("rsin-overview-view");

        name = new TextField("Your name");
        sayHello = new Button("Say hello");
        add(name, sayHello);
        setVerticalComponentAlignment(Alignment.END, name, sayHello);
        sayHello.addClickListener(e -> {
            Notification.show("Hello " + name.getValue());
        });


        List<RsinEntrys> rsinList = new ArrayList<>();
        try {
            Map<Integer, Integer> tablesizes = SQLite.getRSINTableSizes();
            for (Map.Entry<Integer, Integer> entry : tablesizes.entrySet()) {
                rsinList.add(new RsinEntrys(entry.getValue(), entry.getKey().toString()));
            }
        } catch (Exception e) {
            java.lang.System.out.println("Failed to start Spring:");
            java.lang.System.out.println(e.getMessage());
        }

        Grid<RsinEntrys> grid = new Grid<>();
        grid.setItems(rsinList);
        grid.addColumn(RsinEntrys::getName).setHeader("Rsin Entry");
        grid.addColumn(RsinEntrys::getId).setHeader("Value");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_ROW_STRIPES);
        add(grid);

        /*
        TreeGrid<RsinEntrys> grid = new TreeGrid<>();
        try {
            Map<Integer, Integer> tablesizes = SQLite.getRSINTableSizes();
            for (Map.Entry<Integer, Integer> entry : tablesizes.entrySet()) {
                entry.getKey().toString(); entry.getValue();
            }
        } catch (Exception e) {

        }

        grid.setItems(departmentData.getRootDepartments(), departmentData::getChildDepartments);
        grid.addComponentHierarchyColumn(
                rsinEntrys -> {
                    Span departmentName = new Span(rsinEntrys.getName());
                    Span managerName = new Span(rsinEntrys.getManager());
                    managerName.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    managerName.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    VerticalLayout departmentLine = new VerticalLayout(departmentName, managerName);
                    departmentLine.setPadding(false);
                    departmentLine.setSpacing(false);
                    return departmentLine;
                }).setHeader("RSIN Entrys");
        add(grid);
        */
    }
}
