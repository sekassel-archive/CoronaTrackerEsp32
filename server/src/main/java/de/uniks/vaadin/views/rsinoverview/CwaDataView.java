package de.uniks.vaadin.views.rsinoverview;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.uniks.cwa.CwaDataInterpreter;
import de.uniks.vaadin.views.main.MainView;
import de.uniks.vaadin.views.viewmodels.RsinEntrys;

import java.util.ArrayList;
import java.util.List;

@Route(value = "rsin", layout = MainView.class)
@PageTitle("RSIN Overview")
@CssImport("./styles/views/rsinoverview/rsin-overview-view.css")
public class CwaDataView extends HorizontalLayout {

    private List<RsinEntrys> rsinList = null;

    public CwaDataView() {
        setId("rsin-overview-view");
        TreeGrid<RsinEntrys> grid = new TreeGrid<>();

        if (rsinList == null) {
            rsinList = new ArrayList<>();
            grid.setItems(rsinList);
        }

        rsinList.clear();
        CwaDataInterpreter.getCwaData().forEach((key, val) -> {
            String rsin = key.toString();
            List<byte[]> tekList = val;
            rsinList.add(new RsinEntrys(rsin, tekList));
        });

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
