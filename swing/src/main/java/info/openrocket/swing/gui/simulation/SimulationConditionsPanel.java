package info.openrocket.swing.gui.simulation;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import info.openrocket.core.document.Simulation;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.models.atmosphere.ExtendedISAModel;
import info.openrocket.core.models.wind.MultiLevelPinkNoiseWindModel;
import info.openrocket.core.models.wind.PinkNoiseWindModel;
import info.openrocket.core.models.wind.WindModelType;
import info.openrocket.core.simulation.DefaultSimulationOptionFactory;
import info.openrocket.core.simulation.SimulationOptions;
import info.openrocket.core.simulation.SimulationOptionsInterface;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.Unit;
import info.openrocket.core.unit.UnitGroup;
import info.openrocket.core.util.StateChangeListener;

import info.openrocket.swing.gui.util.Icons;
import info.openrocket.swing.gui.widgets.IconButton;
import net.miginfocom.swing.MigLayout;
import info.openrocket.swing.gui.SpinnerEditor;
import info.openrocket.swing.gui.adaptors.BooleanModel;
import info.openrocket.swing.gui.adaptors.DoubleModel;
import info.openrocket.swing.gui.components.BasicSlider;
import info.openrocket.swing.gui.components.UnitSelector;

public class SimulationConditionsPanel extends JPanel {
	private static final Translator trans = Application.getTranslator();

	private WindLevelVisualizationDialog visualizationDialog;

	SimulationConditionsPanel(final Simulation simulation) {
		super(new MigLayout("fill"));

		SimulationOptions simulationOptions = simulation.getOptions();

		// Simulation conditions settings
		addSimulationConditionsPanel(this, simulationOptions);

		// Add buttons for restoring and saving defaults
		addDefaultButtons(simulationOptions);
	}

	/**
	 * Adds the simulation conditions panel to the parent panel.
	 * @param parent The parent panel.
	 * @param target The object containing the simulation conditions setters/getters.
	 * @param addAllWindModels if false, only the average wind model will be added.
	 */
	public static void addSimulationConditionsPanel(JPanel parent, SimulationOptionsInterface target,
													boolean addAllWindModels) {
		JPanel sub;
		DoubleModel pressureModel;
		DoubleModel m;
		JSpinner spin;
		DoubleModel temperatureModel;
		String tip;
		BasicSlider slider;
		UnitSelector unit;

		//// Wind settings:  Average wind speed, turbulence intensity, std. deviation, and direction
		sub = new JPanel(new MigLayout("fill, ins 20 20 0 20", "[grow]", ""));
		//// Wind
		sub.setBorder(BorderFactory.createTitledBorder(trans.get("simedtdlg.lbl.Wind")));
		parent.add(sub, "growx, split 2, aligny 0, flowy, gapright para");

		// Add wind model selection and configuration panel
		if (addAllWindModels) {
			addWindModelPanel(sub, target);
		} else {
			addAverageWindSettings(sub, target);
		}

		//// Temperature and pressure
		sub = new JPanel(new MigLayout("gap rel unrel",
				"[][85lp!][35lp!][75lp!]", ""));
		//// Atmospheric conditions
		sub.setBorder(BorderFactory.createTitledBorder(trans.get("simedtdlg.border.Atmoscond")));
		parent.add(sub, "growx, aligny 0, gapright para");


		BooleanModel isa = new BooleanModel(target, "ISAAtmosphere");
		JCheckBox check = new JCheckBox(isa);
		//// Use International Standard Atmosphere
		check.setText(trans.get("simedtdlg.checkbox.InterStdAtmosphere"));
		//// <html>Select to use the International Standard Atmosphere model.
		//// <br>This model has a temperature of
		//// and a pressure of
		//// at sea level.
		check.setToolTipText(trans.get("simedtdlg.checkbox.ttip.InterStdAtmosphere1") + " " +
				UnitGroup.UNITS_TEMPERATURE.toStringUnit(ExtendedISAModel.STANDARD_TEMPERATURE) +
				" " + trans.get("simedtdlg.checkbox.ttip.InterStdAtmosphere2") + " " +
				UnitGroup.UNITS_PRESSURE.toStringUnit(ExtendedISAModel.STANDARD_PRESSURE) +
				" " + trans.get("simedtdlg.checkbox.ttip.InterStdAtmosphere3"));
		sub.add(check, "spanx, wrap unrel");

		// Temperature:
		JLabel label = new JLabel(trans.get("simedtdlg.lbl.Temperature"));
		//// The temperature at the launch site.
		tip = trans.get("simedtdlg.lbl.ttip.Temperature");
		label.setToolTipText(tip);
		isa.addEnableComponent(label, false);
		sub.add(label, "gapright 50lp");

		temperatureModel = new DoubleModel(target, "LaunchTemperature", UnitGroup.UNITS_TEMPERATURE, 0);

		spin = new JSpinner(temperatureModel.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		isa.addEnableComponent(spin, false);
		sub.add(spin, "growx");

		unit = new UnitSelector(temperatureModel);
		unit.setToolTipText(tip);
		isa.addEnableComponent(unit, false);
		sub.add(unit, "growx");
		slider = new BasicSlider(temperatureModel.getSliderModel(253.15, 308.15)); // -20 ... 35
		slider.setToolTipText(tip);
		isa.addEnableComponent(slider, false);
		sub.add(slider, "w 75lp, wrap");


		// Pressure:
		label = new JLabel(trans.get("simedtdlg.lbl.Pressure"));
		//// The atmospheric pressure at the launch site.
		tip = trans.get("simedtdlg.lbl.ttip.Pressure");
		label.setToolTipText(tip);
		isa.addEnableComponent(label, false);
		sub.add(label);

		pressureModel = new DoubleModel(target, "LaunchPressure", UnitGroup.UNITS_PRESSURE, 0);

		spin = new JSpinner(pressureModel.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		isa.addEnableComponent(spin, false);
		sub.add(spin, "growx");

		unit = new UnitSelector(pressureModel);
		unit.setToolTipText(tip);
		isa.addEnableComponent(unit, false);
		sub.add(unit, "growx");
		slider = new BasicSlider(pressureModel.getSliderModel(0.950e5, 1.050e5));
		slider.setToolTipText(tip);
		isa.addEnableComponent(slider, false);
		sub.add(slider, "w 75lp, wrap");


		isa.addChangeListener(new StateChangeListener() {
			@Override
			public void stateChanged(EventObject e) {
				temperatureModel.stateChanged(e);
				pressureModel.stateChanged(e);
			}
		});


		//// Launch site conditions
		sub = new JPanel(new MigLayout("fill, gap rel unrel",
				"[grow][65lp!][30lp!][75lp!]", ""));
		//// Launch site
		sub.setBorder(BorderFactory.createTitledBorder(trans.get("simedtdlg.lbl.Launchsite")));
		parent.add(sub, "growx, split 2, aligny 0, flowy");


		// Latitude:
		label = new JLabel(trans.get("simedtdlg.lbl.Latitude"));
		//// <html>The launch site latitude affects the gravitational pull of Earth.<br>
		//// Positive values are on the Northern hemisphere, negative values on the Southern hemisphere.
		tip = trans.get("simedtdlg.lbl.ttip.Latitude");
		label.setToolTipText(tip);
		sub.add(label);

		m = new DoubleModel(target, "LaunchLatitude", UnitGroup.UNITS_LATITUDE, -90, 90);

		spin = new JSpinner(m.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		sub.add(spin, "w 65lp!");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		slider = new BasicSlider(m.getSliderModel());
		slider.setToolTipText(tip);
		sub.add(slider, "w 75lp, wrap");


		// Longitude:
		label = new JLabel(trans.get("simedtdlg.lbl.Longitude"));
		tip = trans.get("simedtdlg.lbl.ttip.Longitude");
		label.setToolTipText(tip);
		sub.add(label);

		m = new DoubleModel(target, "LaunchLongitude", UnitGroup.UNITS_LONGITUDE, -180, 180);

		spin = new JSpinner(m.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		sub.add(spin, "w 65lp!");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		slider = new BasicSlider(m.getSliderModel());
		slider.setToolTipText(tip);
		sub.add(slider, "w 75lp, wrap");


		// Altitude:
		label = new JLabel(trans.get("simedtdlg.lbl.Altitude"));
		//// <html>The launch altitude above mean sea level.<br>
		//// This affects the position of the rocket in the atmospheric model.
		tip = trans.get("simedtdlg.lbl.ttip.Altitude");
		label.setToolTipText(tip);
		sub.add(label);

		m = new DoubleModel(target, "LaunchAltitude", UnitGroup.UNITS_DISTANCE, 0);

		spin = new JSpinner(m.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		sub.add(spin, "w 65lp!");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		slider = new BasicSlider(m.getSliderModel(0, 250, 1000));
		slider.setToolTipText(tip);
		sub.add(slider, "w 75lp, wrap");


		//// Launch rod
		sub = new JPanel(new MigLayout("fill, gap rel unrel",
				"[grow][75lp!][30lp!][75lp!]", ""));
		//// Launch rod
		sub.setBorder(BorderFactory.createTitledBorder(trans.get("simedtdlg.border.Launchrod")));
		parent.add(sub, "growx, aligny 0, wrap");


		// Length:
		label = new JLabel(trans.get("simedtdlg.lbl.Length"));
		//// The length of the launch rod.
		tip = trans.get("simedtdlg.lbl.ttip.Length");
		label.setToolTipText(tip);
		sub.add(label);

		m = new DoubleModel(target, "LaunchRodLength", UnitGroup.UNITS_LENGTH, 0);

		spin = new JSpinner(m.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		sub.add(spin, "growx");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		slider = new BasicSlider(m.getSliderModel(0, 1, 5));
		slider.setToolTipText(tip);
		sub.add(slider, "w 75lp, wrap");

		// Keep launch rod parallel to the wind.

		BooleanModel intoWind = new BooleanModel(target, "LaunchIntoWind");
		JCheckBox checkWind = new JCheckBox(intoWind);
		//// Use International Standard Atmosphere
		checkWind.setText(trans.get("simedtdlg.checkbox.Intowind"));
		checkWind.setToolTipText(
				trans.get("simedtdlg.checkbox.ttip.Intowind1") +
				trans.get("simedtdlg.checkbox.ttip.Intowind2") +
				trans.get("simedtdlg.checkbox.ttip.Intowind3") +
				trans.get("simedtdlg.checkbox.ttip.Intowind4"));
		sub.add(checkWind, "spanx, wrap unrel");


		// Angle:
		label = new JLabel(trans.get("simedtdlg.lbl.Angle"));
		//// The angle of the launch rod from vertical.
		tip = trans.get("simedtdlg.lbl.ttip.Angle");
		label.setToolTipText(tip);
		sub.add(label);

		m = new DoubleModel(target, "LaunchRodAngle", UnitGroup.UNITS_ANGLE,
				-SimulationOptions.MAX_LAUNCH_ROD_ANGLE, SimulationOptions.MAX_LAUNCH_ROD_ANGLE);

		spin = new JSpinner(m.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		spin.setToolTipText(tip);
		sub.add(spin, "growx");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		slider = new BasicSlider(m.getSliderModel(-SimulationOptions.MAX_LAUNCH_ROD_ANGLE, 0,
				SimulationOptions.MAX_LAUNCH_ROD_ANGLE));
		slider.setToolTipText(tip);
		sub.add(slider, "w 75lp, wrap");


		// Direction:
		JLabel directionLabel = new JLabel(trans.get("simedtdlg.lbl.Direction"));
		//// <html>Direction of the launch rod.
		tip = trans.get("simedtdlg.lbl.ttip.Direction1") +
				UnitGroup.UNITS_ANGLE.toStringUnit(0) +
				" " + trans.get("simedtdlg.lbl.ttip.Direction2") + " " +
				UnitGroup.UNITS_ANGLE.toStringUnit(2*Math.PI) +
				" " + trans.get("simedtdlg.lbl.ttip.Direction3");
		directionLabel.setToolTipText(tip);
		sub.add(directionLabel);

		m = new DoubleModel(target, "LaunchRodDirection", 1.0, UnitGroup.UNITS_ANGLE,
				0, 2*Math.PI);

		JSpinner directionSpin = new JSpinner(m.getSpinnerModel());
		directionSpin.setEditor(new SpinnerEditor(directionSpin));
		directionSpin.setToolTipText(tip);
		sub.add(directionSpin, "growx");

		unit = new UnitSelector(m);
		unit.setToolTipText(tip);
		sub.add(unit, "growx");
		BasicSlider directionSlider = new BasicSlider(m.getSliderModel(0, 2*Math.PI));
		directionSlider.setToolTipText(tip);
		sub.add(directionSlider, "w 75lp, wrap");
		intoWind.addEnableComponent(directionLabel, false);
		intoWind.addEnableComponent(directionSpin, false);
		intoWind.addEnableComponent(unit, false);
		intoWind.addEnableComponent(directionSlider, false);
	}

	public static void addSimulationConditionsPanel(JPanel parent, SimulationOptionsInterface target) {
		addSimulationConditionsPanel(parent, target, true);
	}

	private static void addWindModelPanel(JPanel panel, SimulationOptionsInterface target) {
		ButtonGroup windModelGroup = new ButtonGroup();

		// Wind model to use
		panel.add(new JLabel(trans.get("simedtdlg.lbl.WindModelSelection")), "spanx, split 3, gapright para");

		//// Average
		JRadioButton averageButton = new JRadioButton(trans.get("simedtdlg.radio.AverageWind"));
		averageButton.setToolTipText(trans.get("simedtdlg.radio.AverageWind.ttip"));
		averageButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

		//// Multi-level
		JRadioButton multiLevelButton = new JRadioButton(trans.get("simedtdlg.radio.MultiLevelWind"));
		multiLevelButton.setToolTipText(trans.get("simedtdlg.radio.MultiLevelWind.ttip"));

		windModelGroup.add(averageButton);
		windModelGroup.add(multiLevelButton);

		panel.add(averageButton);
		panel.add(multiLevelButton, "wrap");

		panel.add(new JSeparator(JSeparator.HORIZONTAL), "spanx, growx, wrap");

		JPanel windSettingsPanel = new JPanel(new CardLayout());

		JPanel averagePanel = new JPanel(new MigLayout("fill, ins 0", "[grow][75lp!][30lp!][75lp!]", ""));
		JPanel multiLevelPanel = new JPanel(new MigLayout("fill, ins 0"));

		addAverageWindSettings(averagePanel, target);
		addMultiLevelSettings(multiLevelPanel, target);

		windSettingsPanel.add(averagePanel, "Average");
		windSettingsPanel.add(multiLevelPanel, "MultiLevel");

		panel.add(windSettingsPanel, "grow, wrap");

		averageButton.addActionListener(e -> {
			((CardLayout) windSettingsPanel.getLayout()).show(windSettingsPanel, "Average");
			if (target instanceof SimulationOptions) {
				((SimulationOptions) target).setWindModelType(WindModelType.AVERAGE);
			}
		});

		multiLevelButton.addActionListener(e -> {
			((CardLayout) windSettingsPanel.getLayout()).show(windSettingsPanel, "MultiLevel");
			if (target instanceof SimulationOptions) {
				((SimulationOptions) target).setWindModelType(WindModelType.MULTI_LEVEL);
			}
		});

		// Set initial selection based on current wind model
		if (target instanceof SimulationOptions) {
			SimulationOptions options = (SimulationOptions) target;
			if (options.getWindModelType() == WindModelType.AVERAGE) {
				averageButton.setSelected(true);
				((CardLayout) windSettingsPanel.getLayout()).show(windSettingsPanel, "Average");
			} else {
				multiLevelButton.setSelected(true);
				((CardLayout) windSettingsPanel.getLayout()).show(windSettingsPanel, "MultiLevel");
			}
		}
	}

	private static void addAverageWindSettings(JPanel panel, SimulationOptionsInterface target) {
		PinkNoiseWindModel model = target.getAverageWindModel();

		// Wind average
		final DoubleModel windSpeedAverage = addDoubleModel(panel, "Averwindspeed", trans.get("simedtdlg.lbl.ttip.Averwindspeed"), model, "Average",
				UnitGroup.UNITS_WINDSPEED, 0, 10.0);

		// Wind standard deviation
		final DoubleModel windSpeedDeviation = addDoubleModel(panel, "Stddeviation", trans.get("simedtdlg.lbl.ttip.Stddeviation"),
				model, "StandardDeviation", UnitGroup.UNITS_WINDSPEED, 0,
				new DoubleModel(model, "Average", 0.25, UnitGroup.UNITS_COEFFICIENT, 0));

		windSpeedAverage.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				windSpeedDeviation.stateChanged(e);
			}
		});

		// Turbulence intensity
		String tip = trans.get("simedtdlg.lbl.ttip.Turbulenceintensity1") +
				trans.get("simedtdlg.lbl.ttip.Turbulenceintensity2") + " " +
				UnitGroup.UNITS_RELATIVE.getDefaultUnit().toStringUnit(0.05) +
				" " + trans.get("simedtdlg.lbl.ttip.Turbulenceintensity3") + " " +
				UnitGroup.UNITS_RELATIVE.getDefaultUnit().toStringUnit(0.20) + ".";
		final DoubleModel windTurbulenceIntensity = addDoubleModel(panel, "Turbulenceintensity", tip, model,
				"TurbulenceIntensity", UnitGroup.UNITS_RELATIVE, 0, 1.0, true);

		final JLabel intensityLabel = new JLabel(
				getIntensityDescription(target.getAverageWindModel().getTurbulenceIntensity()));
		intensityLabel.setToolTipText(tip);
		panel.add(intensityLabel, "w 75lp, skip 1, wrap");
		windTurbulenceIntensity.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				intensityLabel.setText(
						getIntensityDescription(target.getAverageWindModel().getTurbulenceIntensity()));
				windSpeedDeviation.stateChanged(e);
			}
		});
		windSpeedDeviation.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				windTurbulenceIntensity.stateChanged(e);
			}
		});

		// Wind direction
		addDoubleModel(panel, "Winddirection", trans.get("simedtdlg.lbl.ttip.Winddirection"), model, "Direction",
				UnitGroup.UNITS_ANGLE, 0, 2 * Math.PI);
	}

	private static void addMultiLevelSettings(JPanel panel, SimulationOptionsInterface target) {
		if (!(target instanceof SimulationOptions options)) {
			return;
		}
		MultiLevelPinkNoiseWindModel model = options.getMultiLevelWindModel();

		// Create the levels table
		WindLevelTableModel tableModel = new WindLevelTableModel(model);
		JTable windLevelTable = new JTable(tableModel);
		windLevelTable.setRowSelectionAllowed(false);
		windLevelTable.setColumnSelectionAllowed(false);
		windLevelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		windLevelTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scrolling

		// Set up value columns
		SelectAllCellEditor selectAllEditor = new SelectAllCellEditor();
		ValueCellRenderer valueCellRenderer = new ValueCellRenderer();
		for (int i = 0; i < windLevelTable.getColumnCount(); i += 2) {
			windLevelTable.getColumnModel().getColumn(i).setCellRenderer(valueCellRenderer);
			windLevelTable.getColumnModel().getColumn(i).setCellEditor(selectAllEditor);
		}

		// Set up unit selector columns
		for (int i = 1; i < windLevelTable.getColumnCount(); i += 2) {
			windLevelTable.getColumnModel().getColumn(i).setCellRenderer(new UnitSelectorRenderer());
			windLevelTable.getColumnModel().getColumn(i).setCellEditor(new UnitSelectorEditor());
		}

		// Adjust column widths
		adjustColumnWidths(windLevelTable);

		windLevelTable.setRowHeight(windLevelTable.getRowHeight() + 10);

		// Set up sorting
		TableRowSorter<WindLevelTableModel> sorter = new TableRowSorter<>(tableModel);
		windLevelTable.setRowSorter(sorter);
		sorter.setSortable(0, true);
		for (int i = 1; i < windLevelTable.getColumnCount(); i++) {
			sorter.setSortable(i, false);
		}
		sorter.addRowSorterListener(new RowSorterListener() {
			@Override
			public void sorterChanged(RowSorterEvent e) {
				model.sortLevels();
			}
		});
		sorter.setComparator(0, Comparator.comparingDouble(a -> (Double) a));
		sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

		JScrollPane scrollPane = new JScrollPane(windLevelTable);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(400, 150));

		panel.add(scrollPane, "grow, wrap");

		//// Buttons
		JPanel buttonPanel = new JPanel(new MigLayout("ins 0"));

		// Add level
		JButton addButton = new IconButton(trans.get("simedtdlg.but.addWindLevel"), Icons.FILE_NEW);
		addButton.addActionListener(e -> {
			tableModel.addWindLevel();
			sorter.sort();
		});
		buttonPanel.add(addButton);

		// Remove level
		JButton removeButton = new IconButton(trans.get("simedtdlg.but.removeWindLevel"), Icons.EDIT_DELETE);
		removeButton.addActionListener(e -> {
			int selectedRow = windLevelTable.getSelectedRow();
			tableModel.removeWindLevel(selectedRow);
			sorter.sort();
		});
		buttonPanel.add(removeButton, "gapright unrel");

		// Visualization levels
		JButton visualizeButton = new IconButton(trans.get("simedtdlg.but.visualizeWindLevels"), Icons.SIM_PLOT);
		visualizeButton.addActionListener(e -> {
			Window owner = SwingUtilities.getWindowAncestor(panel);
			if (owner instanceof Dialog) {
				WindLevelVisualizationDialog visualizationDialog = new WindLevelVisualizationDialog(
						(Dialog) owner,
						model,
						tableModel.getCurrentUnits()[0],
						tableModel.getCurrentUnits()[1]
				);
				tableModel.setVisualizationDialog(visualizationDialog);
				visualizationDialog.setVisible(true);
			}
		});
		buttonPanel.add(visualizeButton);

		panel.add(buttonPanel, "grow, wrap");

		// Add listener to update visualization when table data changes
		tableModel.addTableModelListener(e -> {
			sorter.sort();
			if (tableModel.getVisualizationDialog() != null) {
				tableModel.getVisualizationDialog().repaint();
			}
		});
	}

	private static DoubleModel addDoubleModel(JPanel panel, String labelKey, String tooltipText, Object source, String sourceKey,
									   UnitGroup unit, double min, Object max, boolean easterEgg) {
		JLabel label = new JLabel(trans.get("simedtdlg.lbl." + labelKey));
		panel.add(label);

		DoubleModel model;
		if (max instanceof Double) {
			model = new DoubleModel(source, sourceKey, unit, min, (Double) max);
		} else if (max instanceof DoubleModel) {
			model = new DoubleModel(source, sourceKey, unit, min, (DoubleModel) max);
		} else {
			throw new IllegalArgumentException("Invalid max value");
		}

		JSpinner spin = new JSpinner(model.getSpinnerModel());
		spin.setEditor(new SpinnerEditor(spin));
		if (tooltipText != null) {
			spin.setToolTipText(tooltipText);
		}
		panel.add(spin, "growx");

		if (easterEgg) {
			addEasterEgg(spin, panel);
		}

		UnitSelector unitSelector = new UnitSelector(model);
		panel.add(unitSelector, "growx");

		BasicSlider slider = new BasicSlider(model.getSliderModel());
		panel.add(slider, "w 75lp, wrap");

		return model;
	}

	private static DoubleModel addDoubleModel(JPanel panel, String labelKey, String tooltipText, Object source, String sourceKey,
											  UnitGroup unit, double min, Object max) {
		return addDoubleModel(panel, labelKey, tooltipText, source, sourceKey, unit, min, max, false);
	}

	private static void adjustColumnWidths(JTable table) {
		TableColumnModel columnModel = table.getColumnModel();
		for (int column = 0; column < table.getColumnCount(); column++) {
			TableColumn tableColumn = columnModel.getColumn(column);
			int preferredWidth = getPreferredColumnWidth(table, column);
			preferredWidth = column == 0 ? preferredWidth + 20 : preferredWidth;	// Add extra padding to first column (for sorting arrow)
			tableColumn.setPreferredWidth(preferredWidth);
		}
	}

	private static int getPreferredColumnWidth(JTable table, int column) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		// Get width of column header
		TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
		if (headerRenderer == null) {
			headerRenderer = table.getTableHeader().getDefaultRenderer();
		}
		Object headerValue = tableColumn.getHeaderValue();
		Component headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, column);
		int headerWidth = headerComp.getPreferredSize().width;

		// Get maximum width of column data
		int maxWidth = headerWidth;
		for (int row = 0; row < table.getRowCount(); row++) {
			TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
			Component comp = table.prepareRenderer(cellRenderer, row, column);
			maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
		}

		// Add some padding
		return maxWidth + 10;
	}

	private void addDefaultButtons(SimulationOptions options) {
		JButton restoreDefaults = new JButton(trans.get("simedtdlg.but.resettodefault"));
		restoreDefaults.addActionListener(e -> {
			DefaultSimulationOptionFactory f = Application.getInjector().getInstance(DefaultSimulationOptionFactory.class);
			SimulationOptions defaults = f.getDefault();
			options.copyConditionsFrom(defaults);
		});
		this.add(restoreDefaults, "span, split 3, skip, gapright para, right");

		JButton saveDefaults = new JButton(trans.get("simedtdlg.but.savedefault"));
		saveDefaults.addActionListener(e -> {
			DefaultSimulationOptionFactory f = Application.getInjector().getInstance(DefaultSimulationOptionFactory.class);
			f.saveDefault(options);
		});
		this.add(saveDefaults, "gapright para, right");
	}

	private static String getIntensityDescription(double i) {
		if (i < 0.001)
			//// None
			return trans.get("simedtdlg.IntensityDesc.None");
		if (i < 0.05)
			//// Very low
			return trans.get("simedtdlg.IntensityDesc.Verylow");
		if (i < 0.10)
			//// Low
			return trans.get("simedtdlg.IntensityDesc.Low");
		if (i < 0.15)
			//// Medium
			return trans.get("simedtdlg.IntensityDesc.Medium");
		if (i < 0.20)
			//// High
			return trans.get("simedtdlg.IntensityDesc.High");
		if (i < 0.25)
			//// Very high
			return trans.get("simedtdlg.IntensityDesc.Veryhigh");
		//// Extreme
		return trans.get("simedtdlg.IntensityDesc.Extreme");
	}

	/**
	 * Shh, don't tell anyone about this easter-egg. (displays a fun quote when the text of the spinner equals 42)
	 * @param spinner the magic spinner!
	 */
	private static void addEasterEgg(JSpinner spinner, Component parent) {
		JTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				String text = textField.getText() + e.getKeyChar();
				if (text.equals("42")) {
					JOptionPane.showMessageDialog(parent,
							"The answer to the ultimate question of life, the universe, and everything.",
							"42", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
	}

	public void cleanup() {
		// Dispose of the visualization dialog if it exists
		if (visualizationDialog != null) {
			visualizationDialog.dispose();
			visualizationDialog = null;
		}

		// Remove all components from the panel
		removeAll();
	}

	@Override
	public void addNotify() {
		super.addNotify();

		// Now that the panel is added to a container, we can safely get the parent window
		Window parent = SwingUtilities.getWindowAncestor(this);
		if (parent != null) {
			parent.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					cleanup();
				}
			});
		}
	}

	private static class WindLevelTableModel extends AbstractTableModel {
		private final MultiLevelPinkNoiseWindModel model;
		private static final String[] columnNames = {
				trans.get("simedtdlg.col.Altitude"),
				trans.get("simedtdlg.col.Unit"),
				trans.get("simedtdlg.col.Speed"),
				trans.get("simedtdlg.col.Unit"),
				trans.get("simedtdlg.col.Direction"),
				trans.get("simedtdlg.col.Unit"),
				trans.get("simedtdlg.col.StandardDeviation"),
				trans.get("simedtdlg.col.Unit"),
				trans.get("simedtdlg.col.Turbulence"),
				trans.get("simedtdlg.col.Unit"),
				trans.get("simedtdlg.col.Intensity"),
		};
		private static final UnitGroup[] unitGroups = {
				UnitGroup.UNITS_DISTANCE, UnitGroup.UNITS_VELOCITY, UnitGroup.UNITS_ANGLE,
				UnitGroup.UNITS_VELOCITY, UnitGroup.UNITS_RELATIVE};
		private final Unit[] currentUnits = {
				UnitGroup.UNITS_DISTANCE.getDefaultUnit(),
				UnitGroup.UNITS_VELOCITY.getDefaultUnit(),
				UnitGroup.UNITS_ANGLE.getDefaultUnit(),
				UnitGroup.UNITS_VELOCITY.getDefaultUnit(),
				UnitGroup.UNITS_RELATIVE.getDefaultUnit(),
		};
		private WindLevelVisualizationDialog visualizationDialog;

		public WindLevelTableModel(MultiLevelPinkNoiseWindModel model) {
			this.model = model;
		}

		public void setVisualizationDialog(WindLevelVisualizationDialog visualizationDialog) {
			this.visualizationDialog = visualizationDialog;
		}

		public WindLevelVisualizationDialog getVisualizationDialog() {
			return visualizationDialog;
		}

		@Override
		public int getRowCount() {
			return model.getLevels().size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			// Intensity column
			if (columnIndex == getColumnCount()-1) {
				return String.class;
			}
			return (columnIndex % 2 == 0) ? Double.class : Unit.class;
		}

		public Object getSIValueAt(int rowIndex, int columnIndex) {
			MultiLevelPinkNoiseWindModel.LevelWindModel level = model.getLevels().get(rowIndex);
			return switch (columnIndex) {
				case 0 -> level.getAltitude();
				case 2 -> level.getSpeed();
				case 4 -> level.getDirection();
				case 6 -> level.getStandardDeviation();
				case 8 -> level.getTurblenceIntensity();
				default -> null;
			};
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// Intensity column
			if (columnIndex == getColumnCount()-1) {
				return getIntensityDescription(model.getLevels().get(rowIndex).getTurblenceIntensity());
			}
			if (columnIndex % 2 == 0) {
				Object rawValue = getSIValueAt(rowIndex, columnIndex);
				if (rawValue == null) {
					return null;
				}
				return currentUnits[columnIndex / 2].toUnit((double) rawValue);
			} else {
				return currentUnits[columnIndex / 2];
			}
		}

		public Unit[] getCurrentUnits() {
			return currentUnits;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			MultiLevelPinkNoiseWindModel.LevelWindModel level = model.getLevels().get(rowIndex);
			if (columnIndex % 2 == 0) {
				// Value column
				double value = Double.parseDouble((String) aValue);
				switch (columnIndex) {
					case 0:
						level.setAltitude(currentUnits[0].fromUnit(value));
						break;
					case 2:
						// Handle negative speed
						if (value < 0) {
							level.setSpeed(currentUnits[1].fromUnit(Math.abs(value)));
							// Adjust direction by 180 degrees
							level.setDirection((level.getDirection() + Math.PI) % (2 * Math.PI));
						} else {
							level.setSpeed(currentUnits[1].fromUnit(value));
						}
						break;
					case 4:
						level.setDirection(currentUnits[2].fromUnit(value));
						break;
					case 6:
						level.setStandardDeviation(currentUnits[3].fromUnit(value));
						break;
					case 8:
						level.setTurbulenceIntensity(currentUnits[4].fromUnit(value));
						break;
				}
			} else {
				// Unit column
				Unit unit = (Unit) aValue;
				currentUnits[columnIndex / 2] = unit;
				if (visualizationDialog != null) {
					visualizationDialog.updateUnits(currentUnits[0], currentUnits[1]);
				}
			}
			fireTableDataChanged();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex != columnNames.length - 1;		// Intensity column is not editable
		}

		public void addWindLevel() {
			List<MultiLevelPinkNoiseWindModel.LevelWindModel> levels = model.getLevels();
			double newAltitude = levels.isEmpty() ? 0 : levels.get(levels.size() - 1).getAltitude() + 100;
			double newSpeed = levels.isEmpty() ? 5 : levels.get(levels.size() - 1).getSpeed();
			double newDirection = levels.isEmpty() ? Math.PI / 2 : levels.get(levels.size() - 1).getDirection();
			double newDeviation = levels.isEmpty() ? 0.2 : levels.get(levels.size() - 1).getStandardDeviation();

			model.addWindLevel(newAltitude, newSpeed, newDirection, newDeviation);
			fireTableDataChanged();
		}

		public void removeWindLevel(int index) {
			if (index >= 0 && index < model.getLevels().size()) {
				model.removeWindLevelIdx(index);
				fireTableDataChanged();
			}
		}

		public UnitGroup getUnitGroup(int columnIndex) {
			return unitGroups[columnIndex / 2];
		}
	}

	private static class UnitSelectorRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Unit unit = (Unit) value;
			return super.getTableCellRendererComponent(table, unit.getUnit(), isSelected, hasFocus, row, column);
		}
	}

	private static class UnitSelectorEditor extends DefaultCellEditor {
		private final JComboBox<Unit> comboBox;

		public UnitSelectorEditor() {
			super(new JComboBox<>());
			comboBox = (JComboBox<Unit>) getComponent();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			WindLevelTableModel model = (WindLevelTableModel) table.getModel();
			UnitGroup unitGroup = model.getUnitGroup(column);
			comboBox.removeAllItems();
			for (Unit unit : unitGroup.getUnits()) {
				comboBox.addItem(unit);
			}
			comboBox.setSelectedItem(value);
			return comboBox;
		}
	}

	private static class ValueCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Double) {
				WindLevelTableModel model = (WindLevelTableModel) table.getModel();
				Unit unit = model.getCurrentUnits()[column / 2];
				double SIValue = unit.fromUnit((Double) value);
				value = unit.toString(SIValue);
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	private static class SelectAllCellEditor extends DefaultCellEditor {
		private Object cellEditorValue;

		public SelectAllCellEditor() {
			super(new JTextField());
			setClickCountToStart(1);

			JTextField textField = (JTextField) getComponent();
			textField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					stopCellEditing();
				}
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			JTextField textField = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
			cellEditorValue = value;
			SwingUtilities.invokeLater(textField::selectAll);
			return textField;
		}

		@Override
		public boolean stopCellEditing() {
			JTextField textField = (JTextField) getComponent();
			cellEditorValue = textField.getText();
			return super.stopCellEditing();
		}

		@Override
		public Object getCellEditorValue() {
			return cellEditorValue;
		}

		@Override
		public boolean isCellEditable(EventObject e) {
			if (e instanceof MouseEvent) {
				return ((MouseEvent) e).getClickCount() >= getClickCountToStart();
			}
			return super.isCellEditable(e);
		}
	}
}
