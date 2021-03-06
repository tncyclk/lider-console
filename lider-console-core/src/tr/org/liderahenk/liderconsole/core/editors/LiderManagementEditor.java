package tr.org.liderahenk.liderconsole.core.editors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.studio.connection.core.io.StudioNamingEnumeration;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.constants.LiderConstants;
import tr.org.liderahenk.liderconsole.core.dialogs.AgentDetailDialog;
import tr.org.liderahenk.liderconsole.core.dialogs.DnDetailsDialog;
import tr.org.liderahenk.liderconsole.core.dialogs.PolicyDefinitionDialog;
import tr.org.liderahenk.liderconsole.core.dialogs.PolicyExecutionSelectDialog;
import tr.org.liderahenk.liderconsole.core.editorinput.DefaultEditorInput;
import tr.org.liderahenk.liderconsole.core.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.ldap.listeners.LdapConnectionListener;
import tr.org.liderahenk.liderconsole.core.ldap.utils.LdapUtils;
import tr.org.liderahenk.liderconsole.core.model.LiderLdapEntry;
import tr.org.liderahenk.liderconsole.core.model.Policy;
import tr.org.liderahenk.liderconsole.core.model.UserAgent;
import tr.org.liderahenk.liderconsole.core.rest.utils.PolicyRestUtils;
import tr.org.liderahenk.liderconsole.core.rest.utils.UserRestUtils;
import tr.org.liderahenk.liderconsole.core.utils.SWTResourceManager;
import tr.org.liderahenk.liderconsole.core.widgets.LiderConfirmBox;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;
import tr.org.liderahenk.liderconsole.core.xmpp.XMPPClient;

/**
 * Lider task and profiles managed by this class. Triggered when entry selected.
 * 
 */
public class LiderManagementEditor extends EditorPart {

	private static final RGB RGB_SELECTED = new RGB(0, 220, 220);

	private static final RGB RGB_DEFAULT = new RGB(245, 255, 255);

	private static final Logger logger = LoggerFactory.getLogger(LiderManagementEditor.class);
	
	private static List<LiderLdapEntry> selectedEntries;
	private static List<LiderLdapEntry> selectedEntriesForTask;
	
	public static String selectedUserDn;

	private Font font = SWTResourceManager.getFont("Noto Sans", 10, SWT.BOLD);

	protected DecoratingLabelProvider decoratingLabelProvider;
	private ScrolledComposite sc;
	private DefaultEditorInput editorInput;
	private TableViewer dnListTableViewer;
	private Label lbDnInfo;
	private Group groupTask;
	private Group groupPolicy;
	
	public static String selectedDn;
	public static List<String> selectedDnUserList;

	private Table table;
	private Table tablePolicyList;

	private TableViewer tableViewerPolicyList;
	private TablePolicyFilter tablePolicyFilter;

	private Button btnAddPolicy;
	private Button btnEditPolicy;
	private Button btnDeletePolicy;
	private Button btnRefreshPolicy;

	private Policy selectedPolicy;
	private Button btnExecutePolicy;

	boolean isPardusDeviceOrHasPardusDevice = false;
	boolean isPardusAccount = false;
	boolean isHasGroupOfNames = false;
	boolean isPardusDeviceGroup = false;
	boolean isSelectionSingle = false;
	boolean isSelectionMulti = false;
	boolean isPardusOu = false;

	private Composite compositeTask;
	private Text textSearchTask;

	private List<PluginTaskWrapper> pluginTaskList;
	private Button btnAhenkInfo;
	private Table tableUserAgents;
	private TableViewer tableViewerUserAgents;
	
	private Image onlineUserAgentImage;
	private Image offlineUserAgentImage;
	private Composite compositeInfoButtons;
	private Button btnSetPasswordPolicy;
	private Button btnSetPassword;

	
	public LiderManagementEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
		selectedUserDn=null;
		selectedDnUserList= new ArrayList<>();
		
		setSite(site);
		setInput(input);
		editorInput = (DefaultEditorInput) input;

		fillWithEntries(); // check selected tree component
		
		onlineUserAgentImage = new Image(Display.getDefault(),
				this.getClass().getClassLoader().getResourceAsStream("icons/32/online-mini.png"));
		offlineUserAgentImage = new Image(Display.getDefault(),
				this.getClass().getClassLoader().getResourceAsStream("icons/32/offline-red-mini.png"));
		
	
		
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(final Composite parent) {

		sc = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayout(new GridLayout(1, false));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);

		Composite composite = new Composite(sc, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite compositeAction = new Composite(composite, SWT.BORDER);
		compositeAction.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		compositeAction.setLayout(new GridLayout(4, false));

		lbDnInfo = new Label(compositeAction, SWT.NONE);
		lbDnInfo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lbDnInfo.setText("");
		new Label(compositeAction, SWT.NONE);
		
		compositeInfoButtons = new Composite(compositeAction, SWT.NONE);
		compositeInfoButtons.setLayout(new GridLayout(3, false));
		
		if (isPardusAccount){
			btnSetPassword = new Button(compositeInfoButtons, SWT.NONE);
			btnSetPassword.setText(Messages.getString("set_password")); //$NON-NLS-1$
			btnSetPassword.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					
					final ICommandService commandService = (ICommandService) PlatformUI.getWorkbench()
							.getService(ICommandService.class);
					

					Command command = commandService.getCommand("tr.org.liderahenk.liderconsole.commands.PasswordTask");  //password plugin command id

					try {
						command.executeWithChecks(new ExecutionEvent());
					} catch (Exception e1) {
						logger.error(e1.getMessage(), e1);
					}
				
					
					
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			

			btnSetPasswordPolicy = new Button(compositeInfoButtons, SWT.NONE);
			btnSetPasswordPolicy.setSize(81, 27);
			btnSetPasswordPolicy.setText(Messages.getString("set_password_policy"));
			
			btnSetPasswordPolicy.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					
					final ICommandService commandService = (ICommandService) PlatformUI.getWorkbench()
							.getService(ICommandService.class);
					

					Command command = commandService.getCommand("tr.org.liderahenk.liderconsole.commands.AddPasswordPolicyTask");  //password plugin command id

					try {
						command.executeWithChecks(new ExecutionEvent());
					} catch (Exception e1) {
						logger.error(e1.getMessage(), e1);
					}
				
					
					
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			
		}
		else {

			btnAhenkInfo = new Button(compositeInfoButtons, SWT.NONE);
			btnAhenkInfo.setText(Messages.getString("AHENK_INFO"));
			btnAhenkInfo.setVisible(isPardusDeviceOrHasPardusDevice && isSelectionSingle);
			btnAhenkInfo.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					IStructuredSelection selection = (IStructuredSelection) dnListTableViewer.getSelection();
					Object firstElement = selection.getFirstElement();
					if (firstElement instanceof LiderLdapEntry) {

						LiderLdapEntry selectedEntry = (LiderLdapEntry) firstElement;

						AgentDetailDialog dialog = new AgentDetailDialog(parent.getShell(), selectedEntry.getName());
						dialog.create();
						dialog.selectedTab(0);
						dialog.open();
					}

				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}
		new Label(compositeAction, SWT.NONE);

		dnListTableViewer = new TableViewer(compositeAction, SWT.BORDER | SWT.FULL_SELECTION);
		dnListTableViewer.getTable().setToolTipText("Seçili DN özelliklerini görmek için tıklayınız..");

		table = dnListTableViewer.getTable();
		GridData gd_table = new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1);
		gd_table.heightHint = 58;
		table.setLayoutData(gd_table);
		// scrolledComposite.setContent(table);
		// scrolledComposite.setMinSize(table.computeSize(SWT.DEFAULT,
		// SWT.DEFAULT));
		// configureTableLayout(dnListTableViewer);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		// table.getVerticalBar().setEnabled(true);
		// table.getVerticalBar().setVisible(true);

		// Set content provider
		dnListTableViewer.setContentProvider(new ArrayContentProvider());

		createTableColumns();

		dnListTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {

				IStructuredSelection selection = (IStructuredSelection) dnListTableViewer.getSelection();
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof LiderLdapEntry) {

					LiderLdapEntry selectedEntry = (LiderLdapEntry) firstElement;
					DnDetailsDialog dialog = new DnDetailsDialog(parent.getShell(), selectedEntry);
					dialog.open();
				}

			}
		});

		dnListTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) dnListTableViewer.getSelection();
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof LiderLdapEntry) {

					LiderLdapEntry selectedEntry = (LiderLdapEntry) firstElement;
					DnDetailsDialog dialog = new DnDetailsDialog(parent.getShell(), selectedEntry);
					dialog.open();
				}

			}
		});

		SashForm sashForm = new SashForm(compositeAction, SWT.VERTICAL);
		sashForm.setTextDirection(0);
		sashForm.setSashWidth(2);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		// POLICY AREA

		groupPolicy = new Group(sashForm, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
		groupPolicy.setLayout(new GridLayout(2, false));
		groupPolicy.setText(Messages.getString("policy_list"));

		createPolicyButtonsArea(groupPolicy);
		tableViewerPolicyList = SWTResourceManager.createTableViewer(groupPolicy);
		sashForm.setWeights(new int[] { 1 });
		tablePolicyList = tableViewerPolicyList.getTable();
		tablePolicyList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		tablePolicyFilter = new TablePolicyFilter();
		tableViewerPolicyList.addFilter(tablePolicyFilter);
		tableViewerPolicyList.refresh();

		createPolicyTableColumns();
		populatePolicyTable();

		// Hook up listeners
		tableViewerPolicyList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) tableViewerPolicyList.getSelection();
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof Policy) {
					setSelectedPolicy((Policy) firstElement);
					btnEditPolicy.setEnabled(true);
					btnDeletePolicy.setEnabled(true);
				}
			}
		});

		tableViewerPolicyList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				PolicyDefinitionDialog dialog = new PolicyDefinitionDialog(parent.getShell(), getSelectedPolicy(),
						getSelf());
				dialog.open();
			}
		});


		if (selectedEntries.size() > 0) {
			populateTable(selectedEntries);
			lbDnInfo.setText("Seçili Dn Sayısı : " + selectedEntries.size());
			// liderLdapEntries=liderEntries; // task icin
		}

		
		
		
		
		// task area for only agents, ou which has pardus device or is pardusDeviceGroup
		if (isPardusDeviceOrHasPardusDevice || isHasGroupOfNames || isPardusDeviceGroup) {

			selectedEntriesForTask= selectedEntries;

			createTaskButtonArea(sashForm, null);
		}
		// task area for online users agents,, user must be login this agent
		else {
			// getting agent for user. if user online for agent
			try {

				if (isSelectionSingle) {

					LiderLdapEntry selectedEntry = selectedEntries.get(0);

					List<UserAgent> agents = null;
					
					if(selectedEntry.getSunucuNo()!=null && selectedEntry.getEntryType() == LiderLdapEntry.PARDUS_ACCOUNT ){
						
						String sunucuNo= selectedEntry.getSunucuNo();
						
						String baseDn = LdapConnectionListener.getConnection().getConnectionParameter().getExtendedProperty("ldapbrowser.baseDn");
						
						String filter="(&(objectClass=pardusDevice)(&(sunucuNo="+sunucuNo+")))";
						
						StudioNamingEnumeration enumeration=LdapUtils.getInstance().search(baseDn, filter, new String[] {}, SearchControls.SUBTREE_SCOPE,10, LdapConnectionListener.getConnection(),
								LdapConnectionListener.getMonitor());
						
						 agents = new ArrayList<UserAgent>();
						
						try {
							if (enumeration != null) {
								while (enumeration.hasMore()) {
									SearchResult item = enumeration.next();
									String dn = item.getName();
									UserAgent agent= new UserAgent();
									agent.setAgentDn(dn);
									
									
									boolean isOnline=XMPPClient.getInstance().getOnlineAgentPresenceMap().containsKey(dn);
									
									agent.setIsOnline(isOnline);
									
									
									agents.add(agent);
									
								}
							}
						} catch (NamingException e) {
							logger.error(e.getMessage(), e);
						}
						
						
						
					}
					else{
 
					//if (!agents.isEmpty()) {
						agents = UserRestUtils.getOnlineUserAgent(selectedEntry.getUid());
						
						
					}
					
					if (!agents.isEmpty()) {
						
						UserAgent selectedUserAgent= agents.get(agents.size()-1); //last record 
						
						// set lider ldap entries for plugin task dialogs..  task dialog handlers get lider ldap entries..
						
						selectedEntriesForTask=new ArrayList<>();
						
						selectedEntriesForTask.add(new LiderLdapEntry(selectedUserAgent.getAgentDn(), null, null));
						//liderLdapTaskEntries.add(new LiderLdapEntry(dn, null, null));
						
						
//						List<UserAgent> onlineAgentList= new ArrayList<>();
//						for (UserAgent userAgent : agents) {
//							if(userAgent.getIsOnline()){
//								onlineAgentList.add(userAgent);
//							}
//						}
						
						selectedUserDn = selectedEntry.getName();
						createTaskButtonArea(sashForm, agents);
					}
				}

			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// sashForm.setWeights(new int[] { 1 });
		}

		sc.setContent(composite);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
	}

	private void createTaskButtonArea(SashForm sashForm, List<UserAgent> onlineAgents) {

		groupTask = new Group(sashForm, SWT.NONE | SWT.H_SCROLL | SWT.V_SCROLL);
		GridLayout gridLayout = new GridLayout(3, false);

		groupTask.setLayout(gridLayout);
		groupTask.setText(Messages.getString("task_list"));
		sashForm.setWeights(new int[] { 1, 3 });
		new Label(groupTask, SWT.NONE);
		
		
		if(onlineAgents!=null && onlineAgents.size()>0){
			
				tableViewerUserAgents = new TableViewer(groupTask, SWT.BORDER | SWT.FULL_SELECTION);
				tableUserAgents = tableViewerUserAgents.getTable();
				tableViewerUserAgents.getTable().setToolTipText("Seçili kullanıcının login olduğu ahenk listesi");
				GridData gd_tableUserAgents = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gd_tableUserAgents.heightHint = 61;
				tableUserAgents.setLayoutData(gd_tableUserAgents);
				tableViewerUserAgents.setContentProvider(new ArrayContentProvider());
				tableUserAgents.setHeaderVisible(true);
				tableUserAgents.setLinesVisible(true);
				createUserAgentsTableColumns();
				
			
				
				tableViewerUserAgents.setInput(onlineAgents != null ? onlineAgents : new ArrayList<UserAgent>());
				
				tableViewerUserAgents.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						IStructuredSelection selection = (IStructuredSelection) tableViewerUserAgents.getSelection();
						Object firstElement = selection.getFirstElement();
						if (firstElement instanceof UserAgent) {
							
							selectedEntries=new ArrayList<>();
							
							selectedEntries.add(new LiderLdapEntry(((UserAgent)firstElement).getAgentDn(), null, null));
						}
					}
				});
				
				tableViewerUserAgents.getTable().select(onlineAgents.size()-1);
				
				
		
		
		}

		textSearchTask = new Text(groupTask, SWT.BORDER);
		GridData layoutData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1);
		layoutData.widthHint = 80;
		textSearchTask.setLayoutData(layoutData);

		compositeTask = new Composite(groupTask, GridData.FILL);
		compositeTask.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		compositeTask.setLayout(new GridLayout(4, true));

		textSearchTask.setMessage(Messages.getString("search"));
		textSearchTask.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {

				String taskLabel = textSearchTask.getText();
				if (taskLabel.length() >= 2) {

					if (pluginTaskList != null)
						for (PluginTaskWrapper pluginTaskWrapper : pluginTaskList) {

							if (pluginTaskWrapper.getTaskButton() != null)
								pluginTaskWrapper.getTaskButton()
										.setBackground(SWTResourceManager.getColor(RGB_DEFAULT));

							if (pluginTaskWrapper.getLabel().toUpperCase().contains(taskLabel.toUpperCase())
									&& pluginTaskWrapper.getTaskButton() != null) {
								pluginTaskWrapper.getTaskButton()
										.setBackground(SWTResourceManager.getColor(RGB_SELECTED));
							}
						}
				}

				if (e.keyCode == 8) // retun pressed fill default value 8
				{
					if (pluginTaskList != null)
						for (PluginTaskWrapper pluginTaskWrapper : pluginTaskList) {
							if (pluginTaskWrapper.getTaskButton() != null)
								pluginTaskWrapper.getTaskButton()
										.setBackground(SWTResourceManager.getColor(RGB_DEFAULT));
						}
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});

		setButtonsToButtonTaskComponent();
	}

	private void fillWithEntries() {

		selectedEntries = editorInput.getLiderLdapEntries();

		ArrayList<LiderLdapEntry> liderEntries = new ArrayList<>();

		for (LiderLdapEntry le : selectedEntries) {

			if(le.getChildren() !=null){
				liderEntries.add(le.getChildren());
			}
			
			
			if ((le.getChildrens() != null && le.getChildrens().size() > 0)) {

				liderEntries.addAll(le.getChildrens());
			}
			
			else {
				liderEntries.add(le);
			}

		}
		// for children
		for (LiderLdapEntry le : liderEntries) {

			if (le.getEntryType()==LiderLdapEntry.PARDUS_DEVICE) {
				isPardusDeviceOrHasPardusDevice = true;
			}
			if (le.getEntryType()==LiderLdapEntry.PARDUS_DEVICE_GROUP) {
				isPardusDeviceGroup = true;
			}
			if(le.getEntryType()== LiderLdapEntry.PARDUS_ACCOUNT){
				isPardusAccount=true;
				selectedUserDn=le.getName();
			}
			
			if(le.getEntryType()== LiderLdapEntry.PARDUS_ORGANIZATIONAL_UNIT){
				isPardusOu=true;
			}
			if (le.isHasGroupOfNames())
				isHasGroupOfNames = true;

		}

		if (liderEntries.size() > 1 || isPardusDeviceGroup ) {
			isSelectionMulti = true;
		} else if (liderEntries.size() == 1) {
			isSelectionSingle = true;
		}
		
		if(isPardusOu){
			
			selectedDnUserList= LdapUtils.getInstance().findUsers(selectedEntries.get(0).getName());
			
			selectedUserDn= null;
		}

	}

	/**
	 * Create table columns related to policy database columns.
	 * 
	 */
	private void createTableColumns() {

		// SELECTED DN NAME
		TableViewerColumn dn = SWTResourceManager.createTableViewerColumn(dnListTableViewer,
				Messages.getString("dn_name"), 200);

		dn.getColumn().setAlignment(SWT.LEFT);
		dn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof LiderLdapEntry) {
					return ((LiderLdapEntry) element).getName();
				}
				return Messages.getString("UNTITLED");
			}
		});

	}
	
	/**
	 * Create table columns related to policy database columns.
	 * 
	 */
	private void createUserAgentsTableColumns() {
		
		TableViewerColumn state = SWTResourceManager.createTableViewerColumn(tableViewerUserAgents,
				Messages.getString("status"), 80);
		
		state.getColumn().setAlignment(SWT.LEFT);
		state.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof UserAgent) {
					return  ((UserAgent) element).getIsOnline() ? "Online" :"Offline"  ;
				}
				return Messages.getString("UNTITLED");
			}
			
			@Override
			public Image getImage(Object element) {
				if (element instanceof UserAgent) {
					if (((UserAgent) element).getIsOnline()) 
							{
						return onlineUserAgentImage;
					} else
						return offlineUserAgentImage;
				}
				return null;
			}
		});
		
		
		// SELECTED DN NAME List<UserAgent>
		TableViewerColumn dn = SWTResourceManager.createTableViewerColumn(tableViewerUserAgents,
				Messages.getString("user_agent"), 430);
		
		dn.getColumn().setAlignment(SWT.LEFT);
		dn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof UserAgent) {
					return ((UserAgent) element).getAgentDn();
				}
				return Messages.getString("UNTITLED");
			}
		});
		// SELECTED DN NAME List<UserAgent>
//		TableViewerColumn loginDate = SWTResourceManager.createTableViewerColumn(tableViewerUserAgents,
//				Messages.getString("login_date"), 150);
//		
//		loginDate.getColumn().setAlignment(SWT.LEFT);
//		loginDate.setLabelProvider(new ColumnLabelProvider() {
//			@Override
//			public String getText(Object element) {
//				if (element instanceof UserAgent) {
//					return new SimpleDateFormat("dd-MM-yy h:mm").format(((UserAgent) element).getUserLoginDate());
//				}
//				return Messages.getString("UNTITLED");
//			}
//		});
//		
//		
//		TableViewerColumn ip = SWTResourceManager.createTableViewerColumn(tableViewerUserAgents,
//				Messages.getString("IP_ADDRESS"), 100);
//		
//		ip.getColumn().setAlignment(SWT.LEFT);
//		ip.setLabelProvider(new ColumnLabelProvider() {
//			@Override
//			public String getText(Object element) {
//				if (element instanceof UserAgent) {
//					
//					if(((UserAgent) element).getUserIp() ==null)
//					return ((UserAgent) element).getIp()  ;
//					else if(((UserAgent) element).getUserIp() !=null)
//						return ((UserAgent) element).getIp() +" - "+ ((UserAgent) element).getUserIp() ;
//				}
//				return Messages.getString("UNTITLED");
//			}
//		});
		
	}

	public LiderManagementEditor getSelf() {
		return this;
	}

	public void refreshPolicyArea() {
		populatePolicyTable();
		tableViewerPolicyList.refresh();
	}

	/**
	 * Create add, edit, delete button for the table.
	 * 
	 * @param composite
	 */
	private void createPolicyButtonsArea(final Composite parent) {

		btnExecutePolicy = new Button(groupPolicy, SWT.NONE);

		btnExecutePolicy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				//List<LiderLdapEntry> selectedDnList = getLiderLdapEntries();
				// select dn from profile table for only execute profile 
				List<LiderLdapEntry> selectedDnList = new ArrayList<>();
				
				TableItem[] items = dnListTableViewer.getTable().getItems();;
				for (int i = 0; i < items.length; i++) {
					TableItem tableItem=items[i];
					if(tableItem.getData() instanceof LiderLdapEntry){
						LiderLdapEntry liderLdapEntry=(LiderLdapEntry) tableItem.getData();
					
						selectedDnList.add(liderLdapEntry);
					}
					
				}

				Set<String> dnSet = null;

				if (selectedDnList != null && selectedDnList.size() > 0) {
					dnSet = new HashSet<String>();

					for (LiderLdapEntry liderLdapEntry : selectedDnList) {
						dnSet.add(liderLdapEntry.getName());
					}

				}

				Policy selectedPolicy = getSelectedPolicy();
				PolicyExecutionSelectDialog dialog = new PolicyExecutionSelectDialog(parent.getShell(), dnSet,
						selectedPolicy);
				dialog.create();
				dialog.open();
			}
		});
		btnExecutePolicy.setText(Messages.getString("POLICY_EXECUTE")); //$NON-NLS-1$
		btnExecutePolicy.setFont(font);

		final Composite composite = new Composite(parent, GridData.FILL);
		composite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		composite.setLayout(new GridLayout(4, false));

		btnAddPolicy = new Button(composite, SWT.NONE);
		btnAddPolicy.setToolTipText(Messages.getString("LiderManagementEditor.btnAddPolicy.toolTipText")); //$NON-NLS-1$
		// btnAddPolicy.setText(Messages.getString("ADD"));
		btnAddPolicy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnAddPolicy.setImage(
				SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE, "icons/16/add.png"));
		btnAddPolicy.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PolicyDefinitionDialog dialog = new PolicyDefinitionDialog(Display.getDefault().getActiveShell(),
						getSelf());
				dialog.create();
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		btnEditPolicy = new Button(composite, SWT.NONE);
		btnEditPolicy.setToolTipText(Messages.getString("LiderManagementEditor.btnEditPolicy.toolTipText")); //$NON-NLS-1$
		// btnEditPolicy.setText(Messages.getString("EDIT"));
		btnEditPolicy.setImage(
				SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE, "icons/16/edit.png"));
		btnEditPolicy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnEditPolicy.setEnabled(false);
		btnEditPolicy.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (null == getSelectedPolicy()) {
					Notifier.warning(null, Messages.getString("PLEASE_SELECT_POLICY"));
					return;
				}
				PolicyDefinitionDialog dialog = new PolicyDefinitionDialog(composite.getShell(), getSelectedPolicy(),
						getSelf());
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		btnDeletePolicy = new Button(composite, SWT.NONE);
		btnDeletePolicy.setToolTipText(Messages.getString("LiderManagementEditor.btnDeletePolicy.toolTipText")); //$NON-NLS-1$
		// btnDeletePolicy.setText(Messages.getString("DELETE"));
		btnDeletePolicy.setImage(
				SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE, "icons/16/delete.png"));
		btnDeletePolicy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnDeletePolicy.setEnabled(false);
		btnDeletePolicy.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (null == getSelectedPolicy()) {
					Notifier.warning(null, Messages.getString("PLEASE_SELECT_POLICY"));
					return;
				}
				if (LiderConfirmBox.open(Display.getDefault().getActiveShell(),
						Messages.getString("DELETE_POLICY_TITLE"), Messages.getString("DELETE_POLICY_MESSAGE"))) {
					try {
						PolicyRestUtils.delete(getSelectedPolicy().getId());
						refreshPolicyArea();
					} catch (Exception e1) {
						logger.error(e1.getMessage(), e1);
						Notifier.error(null, Messages.getString("ERROR_ON_DELETE"));
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		btnRefreshPolicy = new Button(composite, SWT.NONE);
		btnRefreshPolicy.setToolTipText(Messages.getString("LiderManagementEditor.btnRefreshPolicy.toolTipText")); //$NON-NLS-1$
		// btnRefreshPolicy.setText(Messages.getString("REFRESH"));
		btnRefreshPolicy.setImage(
				SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE, "icons/16/refresh.png"));
		btnRefreshPolicy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnRefreshPolicy.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshPolicyArea();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	public class TablePolicyFilter extends ViewerFilter {

		private String searchString;

		public void setSearchText(String s) {
			this.searchString = ".*" + s + ".*";
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			Policy policy = (Policy) element;
			return policy.getLabel().matches(searchString) || policy.getDescription().matches(searchString);
		}
	}

	private void populateTable(List<LiderLdapEntry> liderLdapEntries) {
		try {
			dnListTableViewer.setInput(liderLdapEntries != null ? liderLdapEntries : new ArrayList<LiderLdapEntry>());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			Notifier.error(null, Messages.getString("ERROR_ON_LIST"));
		}
	}

	private void setButtonsToButtonTaskComponent() {

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(LiderConstants.EXTENSION_POINTS.TASK_MENU);
		IConfigurationElement[] config = extensionPoint.getConfigurationElements();

		// Command service will be used to trigger handler class related to
		// specified 'profileCommandId'
		final ICommandService commandService = (ICommandService) PlatformUI.getWorkbench()
				.getService(ICommandService.class);

		if (config != null) {
			// Iterate over each extension point provided by plugins

			pluginTaskList = new ArrayList<>();

			for (IConfigurationElement e : config) {

				try {

					// Read extension point attributes
					final String label = e.getAttribute("label");

					final String pluginName = e.getAttribute("pluginName");

					final String pluginVersion = e.getAttribute("pluginVersion");

					final String taskCommandId = e.getAttribute("taskCommandId");

					final String selectionType = e.getAttribute("selectionType");

					final String description = e.getAttribute("description");

					final String imagePath = e.getAttribute("imagePath");

					PluginTaskWrapper pluginTaskWrapper = new PluginTaskWrapper(label, pluginName, pluginVersion,
							taskCommandId, selectionType, description, imagePath);

					pluginTaskList.add(pluginTaskWrapper);

				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}

			// sort task
			pluginTaskList.sort(new Comparator<PluginTaskWrapper>() {

				@Override
				public int compare(PluginTaskWrapper o1, PluginTaskWrapper o2) {

					return o1.getLabel().compareTo(o2.getLabel());
				}
			});

			for (PluginTaskWrapper pluginTaskWrapper : pluginTaskList) {

				if (pluginTaskWrapper.getSelectionType() != null && isSelectionMulti
						&& pluginTaskWrapper.getSelectionType().equals("multi")) {

					addButtonToTaskArea(commandService, pluginTaskWrapper);

				} else if (isSelectionSingle) {
					addButtonToTaskArea(commandService, pluginTaskWrapper);
				}
			}

		}

	}

	private void addButtonToTaskArea(final ICommandService commandService, final PluginTaskWrapper pluginTaskWrapper) {
		Button btnTask = new Button(compositeTask, SWT.NONE);
		btnTask.setFont(font);
		btnTask.setToolTipText(pluginTaskWrapper.getDescription());
		btnTask.setBackground(SWTResourceManager.getColor(RGB_DEFAULT));

		if (pluginTaskWrapper.getImagePath() != null)
			btnTask.setImage(SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE,
					"icons/16/" + pluginTaskWrapper.getImagePath())); // btnTask.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));

		GridData gd_btnNewButton = new GridData(SWT.FILL, SWT.FILL, true, true);
		// gd_btnNewButton.minimumWidth = 230;
		// gd_btnNewButton.minimumHeight = 100;
		btnTask.setLayoutData(gd_btnNewButton);
		btnTask.setText(pluginTaskWrapper.getLabel());

		pluginTaskWrapper.setTaskButton(btnTask);

		btnTask.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Command command = commandService.getCommand(pluginTaskWrapper.getTaskCommandId());

				try {
					command.executeWithChecks(new ExecutionEvent());
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}

	@Override
	public void setFocus() {

	}

	public static List<LiderLdapEntry> getLiderLdapEntries() {
		return selectedEntries;
	}
	public static List<LiderLdapEntry> getLiderLdapEntriesForTask() {
		return selectedEntriesForTask;
	}

	public void setLiderLdapEntries(List<LiderLdapEntry> liderLdapEntries) {
		LiderManagementEditor.selectedEntries = liderLdapEntries;
	}

	/**
	 * Search policy by plugin name and version, then populate specified table
	 * with policy records.
	 * 
	 */
	private void populatePolicyTable() {
		try {
			List<Policy> policies = PolicyRestUtils.list(null, null);
			tableViewerPolicyList.setInput(policies != null ? policies : new ArrayList<Policy>());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			Notifier.error(null, Messages.getString("ERROR_ON_LIST"));
		}
	}

	private void createPolicyTableColumns() {

		// Label
		TableViewerColumn labelColumn = SWTResourceManager.createTableViewerColumn(tableViewerPolicyList,
				Messages.getString("LABEL"), 100);
		labelColumn.getColumn().setAlignment(SWT.LEFT);
		labelColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Policy) {
					return ((Policy) element).getLabel();
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Description
		TableViewerColumn descColumn = SWTResourceManager.createTableViewerColumn(tableViewerPolicyList,
				Messages.getString("DESCRIPTION"), 400);
		descColumn.getColumn().setAlignment(SWT.LEFT);
		descColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Policy) {
					return ((Policy) element).getDescription();
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Create date
		TableViewerColumn createDateColumn = SWTResourceManager.createTableViewerColumn(tableViewerPolicyList,
				Messages.getString("CREATE_DATE"), 150);
		createDateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Policy) {
					return ((Policy) element).getCreateDate() != null
							? SWTResourceManager.formatDate(((Policy) element).getCreateDate())
							: Messages.getString("UNTITLED");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Modify date
		TableViewerColumn modifyDateColumn = SWTResourceManager.createTableViewerColumn(tableViewerPolicyList,
				Messages.getString("MODIFY_DATE"), 150);
		modifyDateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Policy) {
					return ((Policy) element).getModifyDate() != null
							? SWTResourceManager.formatDate(((Policy) element).getModifyDate())
							: Messages.getString("UNTITLED");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Active
		TableViewerColumn activeColumn = SWTResourceManager.createTableViewerColumn(tableViewerPolicyList,
				Messages.getString("ACTIVE"), 10);
		activeColumn.getColumn().setAlignment(SWT.LEFT);
		activeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Policy) {
					return ((Policy) element).isActive() ? Messages.getString("YES") : Messages.getString("NO");
				}
				return Messages.getString("UNTITLED");
			}
		});
	}

	public Policy getSelectedPolicy() {
		return selectedPolicy;
	}

	public void setSelectedPolicy(Policy selectedPolicy) {
		this.selectedPolicy = selectedPolicy;
	}

}