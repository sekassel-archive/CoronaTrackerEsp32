package de.uniks.vaadin.views.mydevice;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.utils.UserVerificationPostgreSql;
import de.uniks.vaadin.crm.security.model.CustomUserDetails;
import de.uniks.vaadin.views.main.MainView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Route(value = "myDeviceVerification", layout = MainView.class)
@PageTitle("My ESP32 Device")
@RouteAlias(value = "", layout = MainView.class)
public class MyDeviceView extends VerticalLayout {

    private static final String POSITIV_INFECTED = "Proofed Infection (positiv)";
    private static final String NEGATIV_INFECTED = "Proofed NO Infection (negativ)";

    public MyDeviceView() {
        setId("my-device-view");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        TextField readonlyField = new TextField();
        readonlyField.setLabel("UUID");
        readonlyField.setValue(currentPrincipalName);
        readonlyField.setReadOnly(true);

        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setLabel("Test Result");
        radioGroup.setItems(POSITIV_INFECTED, NEGATIV_INFECTED);
        radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        radioGroup.setValue("Proofed Infection (positiv)");

        Button submitButton = new Button("Submit Data");
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

        submitButton.addClickListener(event -> {
            CustomUserDetails loginToken = (CustomUserDetails) authentication.getPrincipal();
            loginToken.setExpired();
            if (loginToken.isAccountNonExpired()) {
                radioGroup.setEnabled(false);
                datePicker.setEnabled(false);
                submitButton.setEnabled(false);
                Boolean positiveInfectedState = radioGroup.getValue().equals(POSITIV_INFECTED);
                LocalDateTime pickedDate = datePicker.getValue().atStartOfDay();
                int rsin = CWACryptography.getRollingStartIntervalNumber(pickedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                //TODO: input data to db to trigger actions required for infection case
                UserVerificationPostgreSql verifUsrDB = new UserVerificationPostgreSql();
                verifUsrDB.flagDataInputPickupable(loginToken.getUsername(), loginToken.getPassword(),
                        loginToken.getTimestamp(), rsin, positiveInfectedState);
                Notification.show("Success! Your Data will be processed soon.");
            } else {
                Notification.show("Your session is expired! Reload Page and try again.");
            }
        });

        add(readonlyField);
        add(radioGroup);
        add(datePicker, value);
        add(submitButton);
    }
}
