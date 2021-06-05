package de.uniks.vaadin.views.rsinoverview;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.uniks.cwa.CwaDataInterpreter;
import de.uniks.vaadin.views.main.MainView;
import de.uniks.vaadin.views.viewmodels.RsinEntrys;

import java.util.ArrayList;
import java.util.List;

@Route(value = "rsin", layout = MainView.class)
@PageTitle("RSIN Overview")
@CssImport("./styles/views/rsinoverview/rsin-overview-view.css")
public class CwaDataView extends HorizontalLayout {

    public CwaDataView() {
        setId("rsin-overview-view");

        List<RsinEntrys> rsinList = new ArrayList<>();
        CwaDataInterpreter.getCwaData().forEach((key, val) -> {
            String rsin = key.toString();
            List<byte[]> tekList = val;
            rsinList.add(new RsinEntrys(rsin, tekList));
        });

        if (rsinList.isEmpty()) {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(true);

            Details component = new Details();
            component.setSummaryText("Wait and reload Page. Cwa Data will be available soon.");
            component.addContent(progressBar);
            component.setEnabled(false);
            component.setOpened(true);
            component.addThemeVariants(DetailsVariant.REVERSE, DetailsVariant.FILLED);

            add(component);
        } else {
            TreeGrid<RsinEntrys> grid = new TreeGrid<>();
            grid.setItems(rsinList);

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
