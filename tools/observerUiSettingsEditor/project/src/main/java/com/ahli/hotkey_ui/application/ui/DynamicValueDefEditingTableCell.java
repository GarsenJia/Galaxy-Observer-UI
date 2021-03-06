// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.hotkey_ui.application.ui;

import com.ahli.hotkey_ui.application.model.ValueDef;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class DynamicValueDefEditingTableCell extends TableCell<ValueDef, String> {
	private static final Logger logger = LogManager.getLogger(DynamicValueDefEditingTableCell.class);
	private static final Pattern NUMBER_INPUT_REGEX_PATTERN = Pattern.compile("(?:[-])?\\d{0,7}(?:[.]\\d{0,4})?");
	
	@Override
	protected void updateItem(final String item, final boolean empty) {
		if (empty || item == null) {
			updateItemNull(item, empty);
		} else {
			// check if old value equals new value
			final boolean notEquals = !item.equals(getItem());
			super.updateItem(item, false);
			if (notEquals) {
				updateItemNotEquals(item);
			} else {
				logger.trace("update valuedef-edit table cell - equal");
			}
		}
	}
	
	private void updateItemNull(final String item, final boolean empty) {
		logger.trace("update valuedef-edit table cell - null");
		super.updateItem(item, empty);
		setGraphic(null);
		setText(null);
	}
	
	private void updateItemNotEquals(final String item) {
		logger.trace("update valuedef-edit table cell - newLabel {}", item);
		final ValueDef data = getTableRow().getItem();
		if (data != null) {
			switch (data.getType()) {
				case BOOLEAN -> createBooleanEditor(data);
				case NUMBER -> createNumberEditor(data, item);
				default -> createChoiceOrTextEditor(data, item);
			}
		} else {
			setGraphic(null);
		}
	}
	
	private void createBooleanEditor(final ValueDef data) {
		final ObservableList<String> items = FXCollections.observableArrayList("false", "true");
		final ComboBox<String> comboBox = new ComboBox<>(items);
		comboBox.valueProperty().addListener((observable, oldItem, newItem) -> {
			final String newVal = newItem != null ? newItem : "";
			data.valueProperty().set(newVal);
		});
		
		final String valuePropStr = data.getValue();
		if (!valuePropStr.isEmpty()) {
			comboBox.getSelectionModel().select(valuePropStr);
		} else {
			comboBox.getSelectionModel().select(data.getDefaultValue());
		}
		
		setGraphic(comboBox);
	}
	
	@SuppressWarnings("java:S5411") // Use the primitive boolean expression here
	private void createNumberEditor(final ValueDef data, final String item) {
		final TextField textField = new TextField(item);
		
		textField.focusedProperty().addListener((obs, wasFocussed, isFocussed) -> {
			if (!isFocussed) {
				final TextField control = (TextField) ((ReadOnlyBooleanProperty) obs).getBean();
				final String input = control.getText().trim();
				if (NUMBER_INPUT_REGEX_PATTERN.matcher(input).matches()) {
					data.valueProperty().set(input);
				} else {
					control.setText(data.valueProperty().getValue());
				}
			}
		});
		
		textField.setText(data.getValue());
		
		setGraphic(textField);
	}
	
	private void createChoiceOrTextEditor(final ValueDef data, final String item) {
		final String[] allowedValues = data.getAllowedValues();
		if (allowedValues != null && allowedValues.length > 0) {
			createChoiceEditor(data);
		} else {
			createTextEditor(data, item);
		}
	}
	
	private void createChoiceEditor(final ValueDef data) {
		final ObservableList<String> items = FXCollections.observableArrayList(data.getAllowedValues());
		final ComboBox<String> comboBox = new ComboBox<>(items);
		
		comboBox.valueProperty().addListener((obs, oldItem, newItem) -> {
			final String newVal = newItem != null ? newItem : "";
			data.valueProperty().set(newVal);
		});
		
		final String valuePropStr = data.getValue();
		if (!valuePropStr.isEmpty()) {
			comboBox.getSelectionModel().select(valuePropStr);
		} else {
			comboBox.getSelectionModel().select(data.getDefaultValue());
		}
		
		setGraphic(comboBox);
	}
	
	@SuppressWarnings("java:S5411") // Use the primitive boolean expression here
	private void createTextEditor(final ValueDef data, final String item) {
		final TextField textField = new TextField(item);
		
		textField.focusedProperty().addListener((obs, wasFocussed, isFocussed) -> {
			if (!isFocussed) {
				final TextField control = (TextField) ((ReadOnlyBooleanProperty) obs).getBean();
				data.valueProperty().set(control.getText());
			}
		});
		
		textField.setText(data.getValue());
		
		setGraphic(textField);
	}
}
