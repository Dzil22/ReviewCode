(:
Copyright (c) 2017, Diksha and/or its affiliates. All rights reserved.

:)

declare namespace soapenv="http://schemas.xmlsoap.org/soap/envelope";
declare namespace ent="http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility";
declare namespace saxon="http://saxon.sf.net/";
declare namespace xsl="http://www.w3.org/1999/XSL/Transform";
declare namespace oms="urn:com:metasolv:oms:xmlapi:1";
declare namespace automator = "java:oracle.communications.ordermanagement.automation.plugin.ScriptReceiverContextInvocation";
declare namespace context = "java:com.mslv.oms.automation.TaskContext";
declare namespace log = "java:org.apache.commons.logging.Log";

declare option saxon:output "method=xml";
declare option saxon:output "saxon:indent-spaces=4";

declare variable $automator external;
declare variable $context external;
declare variable $log external;



declare function local:processMessage(
             $UMAX_Response as element()* ) as element()*{ 
 
 
let $vlanAssociation     := $UMAX_Response/*:subIntWithIP
let $bandwidth           := $vlanAssociation/*:bandwidth/text()
let $key                 := $vlanAssociation/*:key/text()
let $name                := $vlanAssociation/*:name/text()
let $status              := $vlanAssociation/*:status/text()
let $vlan                := $vlanAssociation/*:vlan/text()
let $cVlan               := $vlanAssociation/*:cVlan/text()
let $sVlan               := $vlanAssociation/*:sVlan/text()
let $gateway             := $vlanAssociation/*:gateway/text()   
let $ipAddress           := $vlanAssociation/*:ipAddress/text()   
let $netmask             := $vlanAssociation/*:netmask/text()
let $network             := $vlanAssociation/*:network/text()   

return( 
       <OrderDataUpdate xmlns="http://www.metasolv.com/OMS/OrderDataUpdate/2002/10/25">
                  <Add path="/OperationData">    
                       <DeviceInformation>
                        <Type>{'Associate_VLAN'}</Type>
                        <DeviceInfoParam><ParamName>{'BANDWIDTH'}</ParamName><ParamValue>{$bandwidth}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'KEY'}</ParamName><ParamValue>{$key}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'NAME'}</ParamName><ParamValue>{$name}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'status'}</ParamName><ParamValue>{$status}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'VLAN'}</ParamName><ParamValue>{$vlan}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'CVLAN'}</ParamName><ParamValue>{$cVlan}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'SVLAN'}</ParamName><ParamValue>{$sVlan}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'GATEWAY'}</ParamName><ParamValue>{$gateway}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'IPADDRESS'}</ParamName><ParamValue>{$ipAddress}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'NETMASK'}</ParamName><ParamValue>{$netmask}</ParamValue></DeviceInfoParam>
                        <DeviceInfoParam><ParamName>{'NETWORK'}</ParamName><ParamValue>{$network}</ParamValue></DeviceInfoParam>
                       </DeviceInformation>
                   </Add>                     
            </OrderDataUpdate>
   
            
    )        
   

}; 

declare function local:processFailure(
                    $UMAX_Response as element()* )as element()*{

let $errorCode          := $UMAX_Response/uimaxErr/statusCode/text()
let $errorDescription   := fn:replace($UMAX_Response/uimaxErr/message/text(),'"',"")
let $FunctionName   := "TSQFunction"
let $taskName := context:getTaskMnemonic( $context )

return(

      <OrderDataUpdate xmlns="http://www.metasolv.com/OMS/OrderDataUpdate/2002/10/25">
                <UpdatedNodes>
                    <_root>
                      <FaultData>
                        <FaultMessage>
                            <Code>{$errorCode}</Code>
                            <Description>{$errorDescription}</Description>
                         </FaultMessage>
                         <FaultingFunction>{$FunctionName}</FaultingFunction>
                         <FaultingTask>{$taskName}</FaultingTask>     
                      </FaultData>
                    </_root>
                </UpdatedNodes>
            </OrderDataUpdate>

)

};



let $response               := fn:root(.)
let $UMAX_Response          := $response/UIMAX_Response/vlanDetailOutputType
let $orderUpdate            := local:processMessage($UMAX_Response)

let $orderFailureUpdate     := local:processFailure($UMAX_Response)

let $status                 := $UMAX_Response/error/text()

let $orderUpdatePretty      := saxon:serialize($orderUpdate, <xsl:output method='xml' omit-xml-declaration='yes' indent='yes' saxon:indent-spaces='4'/>)

return (
log:info($log , "Inside Response***********************"),
    if ($status="false")
    then (
          log:info($log ,  concat("Associate VLAN-", $orderUpdatePretty)),
          automator:setUpdateOrder($automator,"false"),
          $orderUpdate ,
          context:completeTaskOnExit($context, "success")  
      )
      else (
                log:debug($log ,  concat("Failure-", saxon:serialize($orderFailureUpdate, <xsl:output method='xml' omit-xml-declaration='yes' indent='yes' saxon:indent-spaces='4'/>))),
                automator:setUpdateOrder($automator,"true"),
                $orderFailureUpdate ,
                context:completeTaskOnExit($context, "failure") 
      
      )
     )