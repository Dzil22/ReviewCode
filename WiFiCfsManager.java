package oracle.communications.inventory.c2a.wifi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.SimpleValue;

import oracle.communications.inventory.api.businessinteraction.BusinessInteractionManager;
import oracle.communications.inventory.api.common.AttachmentManager;
import oracle.communications.inventory.api.entity.BusinessInteraction;
import oracle.communications.inventory.api.entity.PartyServiceRel;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceAssignment;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.entity.common.Involvement;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.api.util.Utils;
import oracle.communications.inventory.c2a.astinet.workers.WIFIInteractionWorker;
import oracle.communications.inventory.c2a.custom.CustomDesignManager;
import oracle.communications.inventory.c2a.impl.ConfigurationUtils;
import oracle.communications.inventory.c2a.wifi.workers.WIFIConfigurationWorker;
import oracle.communications.inventory.c2a.wifi.workers.WIFIServiceWorker;
import oracle.communications.inventory.c2a.wifi.workers.WIFIXmlbeanWorker;
import oracle.communications.inventory.extensibility.extension.util.ExtensionPointContext;
import oracle.communications.inventory.xmlbeans.BusinessInteractionItemType;
import oracle.communications.inventory.xmlbeans.ParameterType;
import oracle.communications.inventory.xmlbeans.PartyType;
import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;

public class WiFiCfsManager {
	private ExtensionPointContext context = null;
	private CustomDesignManager designManager = new CustomDesignManager();
	boolean ismove = false;

	protected Log log = LogFactory.getLog(WiFiCfsManager.class);
	Finder find;

	public WiFiCfsManager(ExtensionPointContext context, Log log) {
		//this.log = log;
		this.context = context;
	}
	/**
	 * Enum with All Possible Service Actions.
	 * @author Pavani
	 *
	 */
	private static enum SERVICE_ACTIONS {
		CREATE("CREATE"), CHANGE("CHANGE"), DISCONNECT("DISCONNECT"), RESUME("RESUME"), SUSPEND("SUSPEND"), 
		SUSPENDWITHCONFIGURATION("SUSPENDWITHCONFIGURATION"), RESUMEWITHCONFIGURATION("RESUMEWITHCONFIGURATION"),
		ADD("ADD"), DELETE("DELETE"), REMOVE("REMOVE"), MOVE("MOVE"), MOVEDELETE("MOVE-DELETE"), MOVEADD("MOVE-ADD");
		SERVICE_ACTIONS(String input){};
	};
	
	/**
	 * @method processCfsServiceConfiguration
	 * @description Execution will starts from this method. 
	 * All method call of Create/Modify/Suspend/Resume/Disconnect will start from this method.
	 * @param scv
	 * @throws ValidationException
	 * @throws Exception
	 */
	public void processCfsServiceConfiguration(ServiceConfigurationVersion scv) 
			throws ValidationException, Exception {
		log.debug("", "Process start");
		String serviceAction = null;
		BusinessInteractionItemType item = (BusinessInteractionItemType) context.getArguments()[1];
		if(item != null) {
			serviceAction = item.getService().getAction().toUpperCase();
			if(serviceAction.equalsIgnoreCase("Move")) {
				ismove = true;
			}
		}
		log.info("", "Service Action: "+serviceAction);

		switch (SERVICE_ACTIONS.valueOf(serviceAction)) {
		case CREATE:
		case ADD:
		case MOVEADD:
			createCFSConfiguration(scv);
			break;
		case CHANGE:
		case MOVE:
			modifyCFSConfiguration(scv);
			break;
		case DISCONNECT:
		case DELETE:
		case REMOVE:
		case MOVEDELETE:
			disconnectCFSConfiguration(scv);
			break;
		case SUSPEND:
			suspendCFSConfiguration(scv);
			break;
		case RESUME:
			resumeCFSConfiguration(scv);
			break;
		default:
			throw new ValidationException("Unrecognized Service Action");
		} 
	}
	/**
	 * @method createCFSConfiguration
	 * @description To Create New CFS Service Configuration.
	 * @param cfsVersion
	 * @throws ValidationException
	 */
	protected void createCFSConfiguration(ServiceConfigurationVersion cfsVersion) throws ValidationException {
		try {
			System.out.println("Try Block :: Processing WIFI CreateCFSConfiguration : Service Name : " + cfsVersion.getService().getExternalObjectId() + " ");
				find = PersistenceHelper.makeFinder();

				BusinessInteractionItemType itemType = (BusinessInteractionItemType) context.getArguments()[1];
				BusinessInteractionManager biManager = PersistenceHelper.makeBusinessInteractionManager();

				WIFIXmlbeanWorker xmlWorker = new WIFIXmlbeanWorker(find);
				WIFIConfigurationWorker confWorker = new WIFIConfigurationWorker();
				WIFIServiceWorker servWorker=new WIFIServiceWorker(find);
				Map<String, String> NDDetails = xmlWorker.getParameterMap();

				xmlWorker.parseParameterList(itemType.getParameterList());
				System.out.println("the partMap is"+xmlWorker.getPartyMap());
				
				if(xmlWorker.getSubscriber()==null)
					throw new ValidationException("Subscriber is a required Parameter");
				if(xmlWorker.getServiceAddress()==null)
					throw new ValidationException("Service_Address is a required Parameter");
				if(xmlWorker.getParameterMap().get("ReservationId")==null)
					throw new ValidationException("Reservation was not found or not provided");
				if(xmlWorker.getParameterMap().get("Wifi_Package")==null)
					throw new ValidationException("The WiFi package is a required field");
			
				// To add the Service_Status
				
				//to add  party
				List<ParameterType> paramList = itemType.getParameterList();
				PartyType partyType = null;
				String partnerName=null;
				String category=null;
				
				
				for (ParameterType param : paramList) {
				if (param.getName().equalsIgnoreCase("Subscriber")){
					partyType = (PartyType) param.getValue();
				}else if(param.getName().equalsIgnoreCase("Partner_Name")){
					SimpleValue paramValue = ((SimpleValue)designManager.getParamValue(param.getName(), itemType));
					partnerName=paramValue.getStringValue();
					
				}else if(param.getName().equalsIgnoreCase("Wifi_Kategori_Partnership")){
					SimpleValue paramValue = ((SimpleValue)designManager.getParamValue(param.getName(), itemType));
					category=paramValue.getStringValue();
				}
					
				}
				find.reset();
				servWorker.findAndAssociatePartytoService(cfsVersion.getService(), partyType);
				find.reset();
				System.out.println("enter  for  CFS in prtner"+xmlWorker.getSubscriber());
				 
							if(null!=partnerName&&null!=category){
								
								find.reset();
								
								servWorker.findAndAssociatePartnertoService(cfsVersion.getService(), partnerName, category);
								find.reset();
							}
				
				ServiceConfigurationItem rootItem = confWorker.findRootServiceConfigurationItem(cfsVersion);
				
				ServiceConfigurationItem sharedParamsConfigItem = confWorker.findServiceConfigItemByPath(rootItem,
						"Properties");
				confWorker.addUpdateConfigItemCharacteristic(sharedParamsConfigItem, "Service_Status", "New");
				String ND = NDDetails.get("ND");
				if(ND!=null)
					servWorker.setServiceChar(cfsVersion.getService(), "ND", ND);
				

				biManager.switchContext((String) null, null);
				String action = itemType.getService().getAction();
				System.out.println("action : " + action);
				WIFIInteractionWorker interWorker = new WIFIInteractionWorker();
				BusinessInteraction childBi;
				childBi = interWorker.mapResourceFacingServiceRequest("Wifi_RFS", context);

				Service rfsService = ConfigurationUtils.getAssociatedService(childBi);
				ServiceConfigurationItem wifiRFS = designManager.addChildConfigItem(cfsVersion, (ServiceConfigurationItem) cfsVersion.getConfigItemTypeConfig(), "Wifi_Service_RFS");
				PersistenceHelper.makeServiceConfigurationManager().assignResource(wifiRFS, rfsService, "", "");

				biManager.switchContext(cfsVersion, null);
				biManager.flushTransaction();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		} finally {
			find.close();
		}
	}
	
	/**
	 * @method modifyCFSConfiguration
	 * @description To Change CFS service configuration.
	 * @param svcConVers
	 * @throws ValidationException
	 */
	protected void modifyCFSConfiguration
	(ServiceConfigurationVersion svcConVers) throws ValidationException {
		System.out.println("Entered Modify WiFi CFS configuration");
		BusinessInteractionItemType itemType = (BusinessInteractionItemType) context.getArguments()[1];
		designManager.mapServiceBusinessInteractionParameters(context);
		find = PersistenceHelper.makeFinder();
		WIFIInteractionWorker interWorker = new WIFIInteractionWorker();
		WIFIConfigurationWorker confWorker = new WIFIConfigurationWorker();
		WIFIServiceWorker servWorker = new WIFIServiceWorker(find);
		WIFIXmlbeanWorker xmlWorker = new WIFIXmlbeanWorker(find);
		xmlWorker.parseParameterList(itemType.getParameterList());
		Map<String, String> reservationProperties = xmlWorker.getParameterMap();
		try {
			/* Getting rfs service and by using rfs and configItem assignSubjectToParent */
			//Missing  in CFS,  defect
			
			ServiceConfigurationItem  configItem = confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Properties").iterator().next();
			confWorker.addUpdateConfigItemCharacteristic(configItem, "Service_Status", "Modified");
			if(reservationProperties.containsKey("Customer_ID")){
				System.out.println(" CFS :: reservationProperties.containsKey..........");
				updatePartyRel(svcConVers, servWorker, xmlWorker, reservationProperties.get("Customer_ID"));
			}
			
				System.out.println("CFS :: Started before updating the new name");			
				String newname=itemType.getService().getExternalIdentity().getExternalObjectId();
				String existingserviceName= svcConVers.getService().getName();
				String rfsServiceAction=null;
				if(!newname.equals(existingserviceName)){
					 rfsServiceAction="Move"; 
				}
				Service existingservice=svcConVers.getService();
//				existingservice.setName(newname);
//				existingservice.setExternalObjectId(newname);
//				existingservice.setExternalName(newname);
				System.out.println("RFS :: Ended after updating the new name");
			
			for(ServiceConfigurationItem rfsConfItem : confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Wifi_Service_RFS"))
				interWorker.mapResourceFacingServiceRequest(((ServiceAssignment) rfsConfItem.getAssignment()).getService().getSpecification().getName(), context);
			
			if(!Utils.checkNull(rfsServiceAction)) {
				System.out.println("CFS :: Started before updating the new name");			
				 newname=itemType.getService().getExternalIdentity().getExternalObjectId();
				 existingservice= svcConVers.getService();
				existingservice.setName(newname);
				/*existingservice.setExternalObjectId(newname);
				existingservice.setExternalName(newname);*/
				System.out.println("RFS :: Ended after updating the new name");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("", "Service modified");
	}
	
	/**
	 * @method disconnectCFSConfiguration
	 * @description To Disconnect the CFS Service Configuration.
	 * @param svcConVers
	 * @throws ValidationException
	 */
	protected void disconnectCFSConfiguration(ServiceConfigurationVersion svcConVers) throws ValidationException {
		try{
			System.out.println("Entered Disconnect WIFI Service CFS Configuration :: Try Block");	
			WIFIConfigurationWorker confWorker = new WIFIConfigurationWorker();
			WIFIInteractionWorker interWorker = new WIFIInteractionWorker();
			BusinessInteractionManager biManager = PersistenceHelper.makeBusinessInteractionManager();

			ServiceConfigurationItem  configItem = confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Properties").iterator().next();
			confWorker.addUpdateConfigItemCharacteristic(configItem, "Service_Status", "Disconnected");

			biManager.flushTransaction();
			biManager.switchContext((String) null, null);

			for(ServiceConfigurationItem rfsConfItem : confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Wifi_Service_RFS"))
				interWorker.mapResourceFacingServiceRequest(((Service) rfsConfItem.getAssignmentsMap().keySet().iterator().next()).getSpecification().getName(), context);

			biManager.switchContext(svcConVers, null);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
		log.debug("", "*********************   WiFi CFS Service Disconnected        *********************");
	}
	
	/**
	 * @method suspendCFSConfiguration
	 * @description To Suspend the CFS Service Configuration.
	 * @param svcConVers
	 * @throws ValidationException
	 */
	protected void suspendCFSConfiguration
	(ServiceConfigurationVersion svcConVers) throws ValidationException {
		System.out.println(" *** Entered Suspend WIFI Service CFS Configuration ***");
		try {
			System.out.println(" *** Entered Suspend WIFI Service CFS Configuration *** :: Try Block");
			WIFIConfigurationWorker confWorker = new WIFIConfigurationWorker();
			BusinessInteractionManager biManager = PersistenceHelper.makeBusinessInteractionManager();

			if(svcConVers.getService().getSuspended())
				throw new ValidationException("A Suspension request cannot be sent to a suspended service.");

			ServiceConfigurationItem  configItem = confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Properties").iterator().next();
			confWorker.addUpdateConfigItemCharacteristic(configItem, "Service_Status", "Suspended");
			Service cfs = svcConVers.getService();
			PersistenceHelper.makeServiceManager().suspendService(cfs);

			WIFIInteractionWorker interWorker = new WIFIInteractionWorker();
			for(ServiceConfigurationItem rfsConfItem : confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Wifi_Service_RFS"))
				interWorker.mapResourceFacingServiceRequest(((ServiceAssignment) rfsConfItem.getAssignment()).getService().getSpecification().getName(), context);
			
			
			

			biManager.flushTransaction();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
		log.debug("", "*********************   WiFi CFS Service suspended        *********************");
	}
	
	/**
	 * @method resumeCFSConfiguration
	 * @description To Resume the CFS Service Configuration.
	 * @param svcConVers
	 * @throws ValidationException
	 */
	protected void resumeCFSConfiguration
	(ServiceConfigurationVersion svcConVers) throws ValidationException {
		try{
			System.out.println("Entered Resume WIFI Service CFS Configuration :: Try Block");
			System.out.println("Entered Resume L3 VPN Service CFS Configuration");
			WIFIConfigurationWorker confWorker = new WIFIConfigurationWorker();
			WIFIInteractionWorker interWorker = new WIFIInteractionWorker();
			BusinessInteractionManager biManager = PersistenceHelper.makeBusinessInteractionManager();

			if(!svcConVers.getService().getSuspended()){
				throw new ValidationException("A Reconnection Request can only be sent to suspended services");
			}

			ServiceConfigurationItem  configItem = confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Properties").iterator().next();
			confWorker.addUpdateConfigItemCharacteristic(configItem, "Service_Status", "Resumed");
			Service cfs = svcConVers.getService();		
			PersistenceHelper.makeServiceManager().resumeService(cfs);

			for(ServiceConfigurationItem rfsConfItem : confWorker.findServiceConfigItemByName(svcConVers.getConfigItems(), "Wifi_Service_RFS"))
				interWorker.mapResourceFacingServiceRequest(((ServiceAssignment) rfsConfItem.getAssignment()).getService().getSpecification().getName(), context);

			biManager.flushTransaction();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
		log.debug("", "*********************   WiFi CFS Service resumed        *********************");
	}
	
	/**
	 * Associating party to service.
	 * @param svcConVers
	 * @param servWorker
	 * @param xmlWorker
	 * @param reservationId
	 * @throws ValidationException
	 */
	private void updatePartyRel(ServiceConfigurationVersion svcConVers, WIFIServiceWorker servWorker,
			WIFIXmlbeanWorker xmlWorker, String reservationId) throws ValidationException {
		Set<PartyServiceRel> parSerRel = svcConVers.getService().getParty();
		String custId = null;
		for (PartyServiceRel partyServiceRel : parSerRel) {
			System.out.println("CFS :: for loop...........");
			custId = partyServiceRel.getParty().getId();
		}
		if(custId!=reservationId){ 
			System.out.println("Updating customer id to the CFS **");
			AttachmentManager AttMgr = PersistenceHelper.makeAttachmentManager();
			Set<PartyServiceRel> partyServiceRel = svcConVers.getService().getParty();
			Collection<Involvement> partyServiceInvolvementColl = new ArrayList<Involvement>();
			for (PartyServiceRel party : partyServiceRel) {
				Involvement partyServiceInvolvement = party;
				partyServiceInvolvementColl.add(partyServiceInvolvement);
			}
			AttMgr.deleteRel(partyServiceInvolvementColl);

			servWorker.findAndAssociatePartytoService(svcConVers.getService(), xmlWorker.getSubscriber());
		}
	}

}
