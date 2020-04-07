package com.orcl.activation.cartridge.alu.sam.x11;

import java.io.ByteArrayOutputStream;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;





import com.mslv.studio.activation.implementation.ILogger;
import com.sun.istack.internal.logging.Logger;



public class SAMHelper {
	
	private ILogger logger = null;

	//Mandatory parameters check
	public boolean isNotNull(String val) throws MandatoryAttributeNotSetException{
		if ( val != null && !val.trim().equals("")){
			return true;
		}else
			throw new MandatoryAttributeNotSetException(val);
		
	}
	
	// Mandatory attributes check
	public void isNotNull(String val, String param) throws MandatoryAttributeNotSetException{
		if ( val != null && !val.trim().equals("")){
			return;
		}else{
			throw new MandatoryAttributeNotSetException("Cannot Find Mandatory Parameter "+param);
		}
	}
	
	// Optional Check
	// Return true if the parameter is not null
	// Return false if the parameter is null
	
	// Optional parameters check
	public boolean isOptValueNotNull(String value){
		if ( value != null && !value.trim().equals(""))
			return true;
		else
			return false;
		
	}
	
	
	 // This method would parse complete SOAPMessage to read ResultCode & ResultDesc
	 public String getError(SOAPMessage soapResp) throws Exception
	 {
	  String error, errorCode, errorDesc = null;

      SOAPEnvelope env = soapResp.getSOAPPart().getEnvelope();
      SOAPBody sb = env.getBody();
      
      // Read ResultCode and ResultDesc. If the response does not contain these tags then 'faultcode' is read
      
      try
      {
	      errorCode = sb.getElementsByTagName(SAMConstants.RESULT_CODE).item(0).getFirstChild().getNodeValue();
	      errorDesc = sb.getElementsByTagName(SAMConstants.RESULT_DESCRIPTION).item(0).getFirstChild().getNodeValue();
	      
		  error = errorCode+SAMConstants.DOUBLE_HYPHEN+errorDesc;
      }
	  catch (java.lang.NullPointerException npEx)
	  {
	      errorCode = sb.getElementsByTagName(SAMConstants.FAULT_CODE).item(0).getFirstChild().getNodeValue();
	      errorDesc = sb.getElementsByTagName(SAMConstants.FAULT_STRING).item(0).getFirstChild().getNodeValue();
	      
		  error = errorCode+SAMConstants.DOUBLE_HYPHEN+errorDesc;
	  }
	  
	  return error;
	 }
	
	 
	 // Create Basic parts of SOAP request
	 public String createSOAPMessageHeader(SAMConnectionHandler conn, String woId) throws Exception
	 {
		 StringBuilder cust = new StringBuilder();
		 
		cust.append(SAMConstants.SOAP_HEADER_START);
		cust.append("<header xmlns=\"xmlapi_1.0\">");
		cust.append("<security>");
		cust.append("<user>"+conn.getUsername()+"</user>");			
		cust.append("<password>"+conn.getPassword()+"</password>");
		cust.append("</security>");
		cust.append("<requestID>newOSS@"+woId+"</requestID>");
		cust.append("</header>");
		cust.append(SAMConstants.SOAP_HEADER_END);
	 
		return cust.toString();
	 }

	 // This method read MTU value from query SAP response
	 public String getPortMTU(String response)
	 {
			String portMTU = null; 
        	
			if(response.contains(SAMConstants.READ_MTU_STRING))
        	{
			//	logger.logDebug("Response contains MTU");
				// split response on basis on line
			//	logger.logDebug("split on line");
		        String []responseRows = response.split("\n");
		        
		     //   logger.logDebug("responseRows length - "+responseRows.length);
		        //search for MTU string in each line;
		        for(int i=1;i<responseRows.length;i++)
		        {
		       // 	logger.logDebug(i+" - "+responseRows[i]);
		        	
		        	// If MTU found, read value and break for loop
		        	if(responseRows[i].indexOf(SAMConstants.READ_MTU_STRING) > 0)
		        	{
		        //		logger.logDebug("Line contains MTU");
		        		String []mtu = responseRows[i].split(SAMConstants.SINGLE_COLON_SPACE);
		        		portMTU=mtu[2];
		        //		logger.logDebug("Value of MTU - "+portMTU);
		        		break;
		        	}
		        };       		
        	}
			
	        return  portMTU;
	 }

	 
/*	 
	 public String createSOAPMessageHeader(SAMConnectionHandler conn, String woId) throws Exception
	 {
		 StringBuilder cust = new StringBuilder();
		 
		cust.append(SAMConstants.SOAP_HEADER_START);
		cust.append("<ns:header xmlns:ns=\"xmlapi_1.0\">");
		cust.append("<ns:security>");
		cust.append("<ns:user>"+conn.getUsername()+"</ns:user>");			
		cust.append("<ns:password>"+conn.getPassword()+"</ns:password>");
		cust.append("</ns:security>");
		cust.append("<ns:requestID>newOSS@"+woId+"</ns:requestID>");
		cust.append("</ns:header>");
		cust.append(SAMConstants.SOAP_HEADER_END);
	 
		return cust.toString();
	 }
*/	 
	 // This method logs SOAP message
	 public void logSoapMessage(SOAPMessage soapMsg, ILogger logger) throws Exception
	 {
		 logger.logDebug("Inside logSoapMessage.");
		 
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 soapMsg.writeTo(baos);
		 logger.logDebug(baos.toString());
	        
	 }
	
	
	 public String generateQosId(String bandwidth)
	 {
		 
		 /*
		  * 
		  	“12010” the means are :
			1 on first digit is package template name
			2 on second digit is unit of bandwidth (1 = Kbps, 2 = Mbps, 3 = Mbps)
			Last 3 digits are amount of bandwidth
			So, 12010 is package-1 and 10 Mbps.


		  */
			StringBuilder qosId = new StringBuilder();
			
			// Template id is default
			qosId.append("1");
			
			// unit of bandwidth - (1 = Kbps, 2 = Mbps, 3 = Mbps)
			if(!(bandwidth.toUpperCase().indexOf("M")==-1))
				qosId.append("2");
			else if(!(bandwidth.toUpperCase().indexOf("K")==-1))
				qosId.append("1");
			else if(!(bandwidth.toUpperCase().indexOf("G")==-1))
				qosId.append("3");
			
			// append value
			qosId.append(bandwidth.substring(0, (bandwidth.length()-1)));
			
			return qosId.toString();
	 }
	 
	 public String findLastOctet(String ipAddress, ILogger logger)
	 {
		 // only IPv4 considered
		 logger.logDebug("Inside Find Last Octet Function");
		 System.out.println("Inside Find Last Octet Function");
		 String[] SDPArray = ipAddress.split("\\.");
		System.out.println("First Value : " + SDPArray[0]);
		System.out.println("Second Value : " + SDPArray[1]);
		System.out.println("Third Value : " + SDPArray[2]);
		System.out.println("Fourth Value : " + SDPArray[3]);

		 String SDP_Value =  SDPArray[3];
		 return SDP_Value;
		 //return ipAddress..split("\\.")[0].trim();
	 }

	 
	 // command example - show service sdp | match 159
/*	 
	 public boolean SDPIdFound(String response, String sdpId)
	 {
			boolean sdpIdFound = false;
	        String []responseRows = response.split("\r\n");
	        
	        for(int i=1;i<responseRows.length;i++)
	        {
	        	if((responseRows[i].indexOf(sdpId) == 0) && (responseRows[i].split(SAMConstants.SPACE)[0].equals(sdpId)))
	        			sdpIdFound = true;
	       	
	        }
	        return  sdpIdFound;
	 }
*/	 
	 public boolean SAPIdFound(String response, String sapId)
	 {
			boolean sdpIdFound = false;
	        String []responseRows = response.split("\r\n");
	        
	        for(int i=1;i<responseRows.length;i++)
	        {
	        	if((responseRows[i].indexOf(sapId) == 0) && (responseRows[i].split(SAMConstants.SPACE)[0].equals(sapId)))
	        			sdpIdFound = true;
	       	
	        }
	        return  sdpIdFound;
	 }
}