package de.uniks.vaadin.views.mydevice;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.uniks.vaadin.crm.security.model.CustomUserDetails;
import de.uniks.vaadin.views.main.MainView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Route(value = "myDeviceVerification", layout = MainView.class)
@PageTitle("My ESP32 Device")
@RouteAlias(value = "", layout = MainView.class)
public class MyDeviceView extends VerticalLayout {

    public MyDeviceView() {
        setId("my-device-view");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        TextField readonlyField = new TextField();
        readonlyField.setLabel("UUID");
        readonlyField.setValue(currentPrincipalName);
        readonlyField.setReadOnly(true);
        add(readonlyField);

        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setLabel("Test Result");
        radioGroup.setItems("Proofed Infection (positiv)", "Proofed NO Infection (negativ)");
        radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroup.setValue("Proofed Infection (positiv)");

        add(radioGroup);

        Button submitButton = new Button("Submit Data", event -> {
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            principal.setExpired();
            //TODO input data to db to trigger actions required for infection case
        });
        submitButton.setEnabled(false);

        DatePicker datePicker = new DatePicker();
        datePicker.setLabel("COVID Test Date");

        Div value = new Div();
        value.setText("Select a value");
        datePicker.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                value.setText("No date selected");
                datePicker.setHelperText("");
                submitButton.setEnabled(false);
            } else {
                LocalDate selectedDate = event.getValue();
                value.setText("Selected date: " + event.getValue());
                if (selectedDate.isAfter(LocalDate.now())) {
                    datePicker.setHelperText("Selected date can not be in the future!");
                    submitButton.setEnabled(false);
                } else if (selectedDate.isBefore(LocalDate.now().minus(14, ChronoUnit.DAYS))) {
                    datePicker.setHelperText("Selected date can not be older than two weeks!");
                    submitButton.setEnabled(false);
                } else {
                    datePicker.setHelperText("");
                    submitButton.setEnabled(true);
                }
            }
        });
        add(datePicker, value);
        add(submitButton);
    }
}
