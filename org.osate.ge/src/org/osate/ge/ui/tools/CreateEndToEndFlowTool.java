package org.osate.ge.ui.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.inject.Named;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.util.IColorConstant;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.EndToEndFlow;
import org.osate.ge.ext.ExtensionConstants;
import org.osate.ge.ext.annotations.Activate;
import org.osate.ge.ext.annotations.CanActivate;
import org.osate.ge.ext.annotations.Deactivate;
import org.osate.ge.ext.annotations.Description;
import org.osate.ge.ext.annotations.Id;
import org.osate.ge.ext.annotations.SelectionChanged;
import org.osate.ge.services.AadlModificationService;
import org.osate.ge.services.BusinessObjectResolutionService;
import org.osate.ge.services.ColoringService;
import org.osate.ge.services.ConnectionService;
import org.osate.ge.services.NamingService;
import org.osate.ge.services.ShapeService;
import org.osate.ge.services.UiService;
import org.osate.ge.services.AadlModificationService.AbstractModifier;
import org.osate.ge.ui.editor.AgeDiagramEditor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osate.aadl2.Aadl2Factory;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.Context;
import org.osate.aadl2.Element;
import org.osate.aadl2.EndToEndFlowElement;
import org.osate.aadl2.EndToEndFlowSegment;
import org.osate.aadl2.FlowEnd;
import org.osate.aadl2.FlowKind;
import org.osate.aadl2.FlowSpecification;
import org.osate.aadl2.ModeFeature;
import org.osate.aadl2.NamedElement;
import org.osate.ge.ui.tools.CreateEndToEndFlowTool;

public class CreateEndToEndFlowTool {
	private ColoringService.Coloring coloring = null;
	private CreateFlowsToolsDialog createEndToEndFlowDialog;
	private ComponentImplementation ci;
	boolean canActivate = true;
	
	@Id
	public final static String ID = "org.osate.ge.ui.tools.CreateEndToEndFlowTool";

	@Description
	public final static String DESCRIPTION = "Create End to End Flow Specification";

	// TODO
	// @Icon
	// public final static ImageDescriptor ICON =
	// Activator.getImageDescriptor("icons/CreateEndToEndFlow.gif");

	@CanActivate
	public boolean canActivate(final IDiagramTypeProvider dtp, final BusinessObjectResolutionService bor) {
		return bor.getBusinessObjectForPictogramElement(dtp.getDiagram()) instanceof ComponentImplementation
				&& canActivate;
	}

	@Activate
	public void activate(final AadlModificationService aadlModService,
			final UiService uiService,
			final ColoringService highlightingService,
			final BusinessObjectResolutionService bor, final IDiagramTypeProvider dtp, final NamingService namingService) {
		final AgeDiagramEditor editor = (AgeDiagramEditor)dtp.getDiagramBehavior().getDiagramContainer();
		editor.getActionRegistry().getAction(CreateEndToEndFlowTool.ID).setEnabled(false);
		// Create a coloring object that will allow adjustment of pictogram
		coloring = highlightingService.adjustColors();
		ci = (ComponentImplementation)bor.getBusinessObjectForPictogramElement(dtp.getDiagram());
		if (ci != null) {
			canActivate = false;
			clearSelection(dtp);
			final Shell dialogShell = new Shell();
			final Rectangle rect = editor.getSite().getShell().getBounds();
			dialogShell.setLocation((rect.x - 150), (rect.y - 50));
			createEndToEndFlowDialog = new CreateFlowsToolsDialog(dialogShell, namingService);
			if (createEndToEndFlowDialog.open() == Dialog.CANCEL) {
				uiService.deactivateActiveTool();
				canActivate = true;
				return;
			}
			
			if (!createEndToEndFlowDialog.getFlows().isEmpty()) {
				aadlModService.modify(ci, new AbstractModifier<ComponentImplementation, Object>() {
					@Override
					public Object modify(final Resource resource, final ComponentImplementation ci) {
						for (EndToEndFlow eteFlow : createEndToEndFlowDialog.getFlows()) {
							ci.getOwnedEndToEndFlows().add(eteFlow);
							ci.setNoFlows(false);
						}
						return null;
					}
				});
			}
			uiService.deactivateActiveTool();
		}
	}

	@Deactivate
	public void deactivate(final IDiagramTypeProvider dtp) {
		final TransactionalEditingDomain editingDomain = dtp.getDiagramBehavior().getEditingDomain();
		editingDomain.getCommandStack().execute(new NonUndoableToolCommand() {
			@Override
			public void execute() {
				// Dispose of the coloring object
				if (coloring != null) {
					if (createEndToEndFlowDialog != null) {
						createEndToEndFlowDialog.close();
					}
					coloring.dispose();
				}
				canActivate = true;
			}
		});

		clearSelection(dtp);
	}
	
	private void clearSelection(final IDiagramTypeProvider dtp) {
		dtp.getDiagramBehavior().getDiagramContainer().selectPictogramElements(new PictogramElement[0]);
		dtp.getDiagramBehavior().refresh();
	}

	@SelectionChanged
	public void onSelectionChanged(@Named(ExtensionConstants.SELECTED_PICTOGRAM_ELEMENTS) final PictogramElement[] selectedPes,
			final BusinessObjectResolutionService bor,final IDiagramTypeProvider dtp, final ShapeService shapeService, final ConnectionService connectionService) {
		// Highlight all selected shapes
		final TransactionalEditingDomain editingDomain = dtp.getDiagramBehavior().getEditingDomain();
		editingDomain.getCommandStack().execute(new NonUndoableToolCommand() {
			@Override
			public void execute() {
				for (PictogramElement pe : selectedPes) {
					Shape shape = null;
					if (pe instanceof Connection) {
						shape = connectionService.getOwnerShape((Connection) pe);
					} else if (pe instanceof ConnectionDecorator) {
						final ConnectionDecorator cd = ((ConnectionDecorator) pe);
						pe = cd.getConnection();
						shape = connectionService.getOwnerShape((Connection) pe);
					}
					final Object bo = bor.getBusinessObjectForPictogramElement(pe);
					final Context context = shapeService.getClosestBusinessObjectOfType(shape, Context.class);
					if (pe != null && createEndToEndFlowDialog != null && createEndToEndFlowDialog.setTargetPictogramElements(bo, ci.getType(), context)) {
						if (bo instanceof ModeFeature) {
							coloring.setForeground(pe, IColorConstant.LIGHT_ORANGE);
						} else {
							coloring.setForeground(pe, IColorConstant.ORANGE);
						}
						createEndToEndFlowDialog.setMessage(getMessage(bo));
					}
				}
			}
		});
	}
	
	private String getMessage(final Object bo) {
		final String addModeFeaturesString = "mode feature if neccessary.";
		if (bo instanceof FlowSpecification) {
			final FlowSpecification fs = (FlowSpecification)bo;
			if (fs.getKind() != FlowKind.SINK) {
				return "Select a valid connection or " + addModeFeaturesString;
			} else {
				return "Select" + addModeFeaturesString;
			}
		} else if (bo instanceof org.osate.aadl2.Connection) {
			return "Select a valid flow specification or " + addModeFeaturesString;
		} else if (bo instanceof ModeFeature) {
			return createEndToEndFlowDialog.getMessage();
		}
		return "Select a valid element.";
	}

	private class CreateFlowsToolsDialog extends TitleAreaDialog {
		final List<EndToEndFlow> flows = new ArrayList<EndToEndFlow>();
		Composite flowSegmentComposite;
		StyledText flowSegmentLabel;
		Text newETEFlowName;
		NamingService namingService;
		final Aadl2Package pkg = Aadl2Factory.eINSTANCE.getAadl2Package();
		private final EndToEndFlow eTEFlow = (EndToEndFlow) pkg.getEFactoryInstance().create(pkg.getEndToEndFlow());
		public CreateFlowsToolsDialog(final Shell parentShell, final NamingService namingService) {
			super(parentShell);
			this.setHelpAvailable(false);
			this.namingService = namingService;
			setShellStyle(SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.ON_TOP | SWT.DIALOG_TRIM | SWT.MIN);
		}

		public List<EndToEndFlow> getFlows() {
			return Collections.unmodifiableList(flows);
		}

		public boolean setTargetPictogramElements(Object bo, ComponentType ciType, Context context) {
				final Element selectedEle = (Element)bo;
				if (isValid(selectedEle, ciType)) {
					if (selectedEle instanceof FlowSpecification || selectedEle instanceof org.osate.aadl2.Connection) {
						addFlowSegment(createEndToEndFlowSegments(selectedEle, (Context)(selectedEle instanceof org.osate.aadl2.Connection ? null : context)));
					} else {
						addFlowSegment(selectedEle);
					}
					return true;
				}
			return false;
		}

		private boolean validFirstElement(final Element selectedEle) {
			return (selectedEle instanceof FlowSpecification) && (((FlowSpecification)selectedEle).getKind() == FlowKind.SOURCE);
		}

		private String getSegmentName(final Context ctx,
				final NamedElement flowElement) {
			String name = "";
			if (ctx != null) {
				name += ctx.getName() == null ? "<unknown>" : ctx.getName();
				name += ".";
			}
			name += flowElement.getName() == null ? "<unknown>" : flowElement
					.getName();
			return name;
		}

		private EndToEndFlowSegment createEndToEndFlowSegments(final Element selectedEle, final Context context) {
			final EndToEndFlowSegment eteFlowSegment = eTEFlow.createOwnedEndToEndFlowSegment();	
			eteFlowSegment.setFlowElement((EndToEndFlowElement) selectedEle);
			eteFlowSegment.setContext(context);
			eTEFlow.getOwnedEndToEndFlowSegments().add(eteFlowSegment);
			return eteFlowSegment;
		}

		final List<String> segmentList = new ArrayList<String>();
		final List<String> modeList = new ArrayList<String>();
		private void addFlowSegment(final Object object) {
			if (!flowSegmentComposite.isDisposed()) {
				flowSegmentLabel.setEnabled(true);
				if (object instanceof EndToEndFlowSegment) {
					final EndToEndFlowSegment eteFlowSegment = (EndToEndFlowSegment)object;
					if (eTEFlow.getAllFlowSegments().size() == 1) {
						segmentList.add(getSegmentName(eteFlowSegment.getContext(), eteFlowSegment.getFlowElement()));
					} else {
						segmentList.add("  ->  " + getSegmentName(eteFlowSegment.getContext(), eteFlowSegment.getFlowElement()));
					}
				} else if (object instanceof ModeFeature) {
					final ModeFeature mf = (ModeFeature)object;
					if (eTEFlow.getInModeOrTransitions().size() == 1) {
						modeList.add("  in modes  (" + mf.getName());
					} else {
						modeList.add(", " + mf.getName());
					}
				}
				
				setFlowImplementationString();
				updateWidgets();
			}
		}
		
		private void setFlowImplementationString() {
			String segmentString = "";
			for (String string : segmentList) {
				 segmentString += string;
			}
			
			flowSegmentLabel.setText(segmentString + getModeString());
			if (eTEFlow.getInModeOrTransitions().size() > 0) {
				final StyleRange styleRange = new StyleRange();
				styleRange.start = segmentString.length();
				styleRange.length = 12;
				styleRange.fontStyle = SWT.BOLD;
				styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
				flowSegmentLabel.setStyleRange(styleRange);
			}
		}
		
		private String getModeString() {
			String modeString = "";
			for (String string : modeList) {
				modeString += string;
			}
			return modeString == "" ? modeString : modeString + ")";
		}

		private void updateWidgets() {
			setNavigationButtonsEnabled(isCompleteAndValid() && !eTEFlow.getName().equals(""));
		}

		private boolean isCompleteAndValid() {
			return (eTEFlow.getAllFlowSegments().size() > 2 && eTEFlow.getAllFlowSegments().size() % 2 == 1
					&& eTEFlow.getAllFlowSegments().get(0).getFlowElement() instanceof FlowSpecification
					&& eTEFlow.getAllFlowSegments().get(eTEFlow.getAllFlowSegments().size()-1).getFlowElement() instanceof FlowSpecification);
		}

		private void setNavigationButtonsEnabled(final boolean enabled) {
			final Button okBtn = getButton(IDialogConstants.OK_ID);
			if (okBtn != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(enabled);
			}
		}
	
		private boolean isValid(final Element selectedEle, final ComponentType ciType) {
				if (selectedEle instanceof ModeFeature) {
					eTEFlow.getInModeOrTransitions().add((ModeFeature)selectedEle);
					return true;
				}
				
				if ((selectedEle instanceof FlowSpecification) && ((FlowSpecification)selectedEle).getNamespace().equals(ciType)) {
					return false;
				}
				
				if ((eTEFlow.getAllFlowSegments().size() == 0)) {
					return validFirstElement(selectedEle);
				}

				final Element prevEle = eTEFlow.getAllFlowSegments().get(eTEFlow.getAllFlowSegments().size()-1).getFlowElement();
				if (prevEle != null) {
					if ((prevEle instanceof org.osate.aadl2.Connection)
							&& (selectedEle instanceof FlowSpecification)) {
						final FlowSpecification segFs = (FlowSpecification)selectedEle;
						if (segFs.getKind() == FlowKind.SINK || segFs.getKind() == FlowKind.PATH) {
							final Object ob = segFs.getInEnd().getFeature();
							final org.osate.aadl2.Connection prevCon = (org.osate.aadl2.Connection)prevEle;
							if (prevCon.isBidirectional() && (((Object)prevCon.getSource().getConnectionEnd()).equals(ob) || ((Object)prevCon.getDestination().getConnectionEnd()).equals(ob))) {
								return true;
							} else if (!prevCon.isBidirectional() && ((Object)prevCon.getDestination().getConnectionEnd()).equals(ob)) {
								return true;
							}
						}
					} else if ((prevEle instanceof FlowSpecification)
							&& (selectedEle instanceof org.osate.aadl2.Connection)) {
						final FlowSpecification segFs = (FlowSpecification)prevEle;
						if (segFs.getKind() == FlowKind.SOURCE || segFs.getKind() == FlowKind.PATH) {
							final Object prevOb = segFs.getOutEnd().getFeature();
							final org.osate.aadl2.Connection segCon = (org.osate.aadl2.Connection)selectedEle;
							if (segCon.isBidirectional() && ((((Object)segCon.getSource().getConnectionEnd()).equals(prevOb) && segCon.getSource().getContext() != null) || (((Object)segCon.getDestination().getConnectionEnd()).equals(prevOb) && segCon.getDestination().getContext() != null))) {
								return true;
							} else if (!segCon.isBidirectional() && ((Object)segCon.getSource().getConnectionEnd()).equals(prevOb) && segCon.getDestination().getContext() != null) {
								return true;
							}
						}
					}
				}
			return false;
		}

		@Override
		protected void configureShell(final Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Create End To End Flow");
			newShell.setSize(475, 275);
		}

		@Override
		public void create() {
			super.create();
			setTitle("Select Elements");
			setMessage("Select start subcomponent flow identifier or mode feature if neccessary.");
		}

		@Override
		protected Control createDialogArea(final Composite parent) {
			final Composite container = (Composite)super.createDialogArea(parent);
			flowSegmentComposite = new Composite(container, SWT.CENTER);
			final RowLayout rowLayout = new RowLayout();
			rowLayout.marginLeft = 10;
			rowLayout.marginTop = 5;
			flowSegmentComposite.setLayout(rowLayout);

			flowSegmentLabel = new StyledText(flowSegmentComposite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
			flowSegmentLabel.setEditable(false);
			flowSegmentLabel.setEnabled(false);
			flowSegmentLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			flowSegmentLabel.setMargins(5, 5, 5, 5);
			final RowData rowData = new RowData();
			rowData.height = 100;
			rowData.width = 415;
			flowSegmentLabel.setLayoutData(rowData);
			flowSegmentLabel.setLayout(new RowLayout());
			
			return container;
		}

		@Override
		protected Control createButtonBar(final Composite parent) {
			final Composite buttonBar = new Composite(parent, SWT.NONE);
			final GridLayout buttonBarLayout = new GridLayout();
			buttonBarLayout.numColumns = 4;
			buttonBar.setLayout(buttonBarLayout);
			final GridData buttonBarData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
			buttonBarData.grabExcessHorizontalSpace = true;
			buttonBarData.grabExcessVerticalSpace = false;
			buttonBar.setLayoutData(buttonBarData);
			buttonBar.setFont(parent.getFont());

			final Label nameLabel = new Label(buttonBar, SWT.NONE);
			nameLabel.setText("Name: ");
			final GridData nameLabelData = new GridData(SWT.LEFT, SWT.CENTER, true, true);
			nameLabelData.grabExcessHorizontalSpace = true;
			nameLabelData.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			nameLabel.setLayoutData(nameLabelData);

			newETEFlowName = new Text(buttonBar, SWT.DEFAULT);
			final GridData nameTextData = new GridData(SWT.LEFT, SWT.CENTER, false, true);
			nameTextData.grabExcessHorizontalSpace = false;
			nameTextData.widthHint = 175;
			newETEFlowName.setLayoutData(nameTextData);
			newETEFlowName.setEditable(true);
			newETEFlowName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					if(!namingService.isValidIdentifier(newETEFlowName.getText()) || namingService.isNameInUse(ci, newETEFlowName.getText())) {
						eTEFlow.setName("");
						updateWidgets();
					} else {
						eTEFlow.setName(newETEFlowName.getText());
						updateWidgets();
					}
				}
			});

			final Control buttonControl = super.createButtonBar(buttonBar);
			buttonControl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
			final Button okBtn = getButton(IDialogConstants.OK_ID);
			okBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					flows.add(eTEFlow);
				}
			});

			if (okBtn != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
			return buttonBar;
		}
	}
}

