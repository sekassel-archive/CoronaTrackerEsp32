package de.uniks.views.rsinoverview;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.TemplateRenderer;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "rsin", layout = MainView.class)
@PageTitle("RSIN Overview")
@CssImport("./styles/views/rsinoverview/rsin-overview-view.css")
@RouteAlias(value = "", layout = MainView.class)
public class RsinOverviewView extends HorizontalLayout {

    private int rsinBuffer;
    private List<RsinEntrys> rsinList = new ArrayList<>();

    public RsinOverviewView() {
        setId("rsin-overview-view");

        TextField rsinInput = new TextField();
        Button addRsinButton = new Button();
        Dialog addRsinTekPopup = new Dialog();
        TextField tekInputField = new TextField();
        Button popupCancelButton = new Button();
        Button popupConfirmButton = new Button();
        TreeGrid<RsinEntrys> grid = new TreeGrid<>();


        // Input field for RSIN, viewed in default view
        rsinInput.setLabel("Add temporary infected RSIN");
        rsinInput.setPattern("[0-9]*");
        rsinInput.setMaxLength(7);
        rsinInput.setPreventInvalidInput(true);
        rsinInput.setPlaceholder("Input have to be 7 numbers");
        rsinInput.setWidth("40em");

        // Popup to add TEK Information with new RSIN number into DB
        tekInputField.setLabel("Add one TEK value set:");
        tekInputField.setPattern("^[0-9,_-]*");
        tekInputField.setPreventInvalidInput(true);
        tekInputField.setPlaceholder("0,39,-3,23,20,-43,-73,-91,114,-47,-47,-8,-57,119,124,93");

        popupCancelButton.setText("Cancel");
        popupCancelButton.addClickListener(e -> addRsinTekPopup.close());

        popupConfirmButton.setText("Confirm");
        popupConfirmButton.addClickListener(e -> {
            byte[] tek = new byte[16];
            String tekInputString = tekInputField.getValue();
            tekInputString = tekInputString.replaceAll("\\s", "");
            String[] tokens = tekInputString.split(",");

            if (tokens.length != 16) {
                Notification.show("TEK data have to be 16 values instead of " + tokens.length + " values.");
            } else {
                if (createRsinEntrysTEK(rsinBuffer, tokens)) {
                    grid.getDataProvider().refreshAll();
                    addRsinTekPopup.close();
                } else {
                    Notification.show("Ups, something went wrong...");
                }
            }
        });

        addRsinTekPopup.add(tekInputField);
        addRsinTekPopup.add(popupCancelButton, popupConfirmButton);

        addRsinTekPopup.setModal(false);
        addRsinTekPopup.setDraggable(true);
        addRsinTekPopup.setResizable(true);
        addRsinTekPopup.setMinHeight("50px");


        addRsinButton.setText("ADD TEK");
        addRsinButton.setEnabled(false);
        addRsinButton.setWidth("10em");
        addRsinButton.addClickListener(e -> {
            if (rsinInput.getValue().length() < 7) {
                Notification.show("RSIN Number is to short.\nThere have to be 7 numbers,\ninstead of "
                        + rsinInput.getValue().length() + " numbers.");
            } else if (rsinList.stream().filter(re -> re.getRsin().equals(rsinInput.getValue())).findAny().isPresent()) {
                Notification.show("RSIN already exists.");
            } else {
                rsinBuffer = Integer.parseInt(rsinInput.getValue());
                addRsinTekPopup.open();
            }
        });

        add(rsinInput, addRsinButton);
        setVerticalComponentAlignment(Alignment.END, rsinInput, addRsinButton);


        // RSIN grid list, with RSIN and number of entrys (clickable column)
        getRsinEntrysList(rsinList);
        try {
            grid.setItems(rsinList);
        } catch (Exception e) {
            // prevent double init
        }

        grid.addColumn(RsinEntrys::getRsin).setHeader("RSIN");
        grid.addColumn(RsinEntrys::getTekEntrys).setHeader("TEK Entrys");
        grid.setHeightByRows(true);
        //grid.setMaxHeight("700px");
        //grid.setHeight("700px");
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

    private List<RsinEntrys> getRsinEntrysList(List<RsinEntrys> colRsinEntrys) {
        // Get RSIN list as string from db
        if (!colRsinEntrys.isEmpty()) {
            colRsinEntrys.clear();
        }
        try {
            Map<Integer, Integer> tablesizes = SQLite.getRSINTableSizes();
            for (Map.Entry<Integer, Integer> entry : tablesizes.entrySet()) {
                colRsinEntrys.add(new RsinEntrys(entry.getValue(), entry.getKey().toString()));
            }
        } catch (Exception e) {
            java.lang.System.out.println("Can't load RSIN List.");
        }
        return colRsinEntrys;
    }

    //addTemporaryExposureKey
    private boolean createRsinEntrysTEK(int rsin, String[] tokens) {
        byte[] tek = new byte[16];
        int i = 0;
        for (String t : tokens) {
            tek[i] = (byte) Integer.parseInt(t);
            i++;
        }

        try {
            SQLite.addTemporaryExposureKey(rsin, tek);
            Notification.show("Added " + rsin + " with one " + tek.length + " Values long Entry.");
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }
}
