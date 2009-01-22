// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ASelectServletFilter.java

package fi.servlet.dwo_runner;

import com.alfaariss.aselect.integration.ASelectException;
import com.alfaariss.aselect.utils.Utils;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;
import javax.servlet.http.*;

// Referenced classes of package com.alfaariss.aselect.integration:
//            ASelectException

public class ASelectServletFilter
{

    public ASelectServletFilter()
    {
        xAppId = null;
        dynamicUrl = null;
        xAppUrl = null;
        xAppProtocol = null;
        xAppDomain = null;
        xAppPort = null;
        xAgentAddress = null;
        xAgentPort = 0;
        xAuthenticationLogFile = null;
        xUseLogging = false;
    }

    public void init(Properties xConfiguration)
        throws ASelectException
    {
        String xMethod = "ASelectFilter.init() -> ";
        if(xConfiguration == null)
            throw new ASelectException(xMethod + "Configuration parameters cannot be null");
        String xTemp = (String)xConfiguration.get("aselect_use_logging");
        if(xTemp == null)
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_use_logging' must be specified");
        if(xTemp.equalsIgnoreCase("true"))
            xUseLogging = true;
        xAuthenticationLogFile = (String)xConfiguration.get("aselect_log_file");
        if(xAuthenticationLogFile == null)
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_log_file' must be specified");
        xAppId = (String)xConfiguration.get("aselect_application_id");
        if(xAppId == null)
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_application_id' must be specified");
        xAppUrl = (String)xConfiguration.get("aselect_application_url");
        if(xAppUrl == null)
        {
            log("aselect_application_url was null -> activating recovery of applicationprotocol, -domain and -port");
            xAppProtocol = (String)xConfiguration.get("aselect_application_protocol");
            if(xAppProtocol == null)
                log("aselect_application_protocol was null -> activating auto recovery of protocol");
            xAppDomain = (String)xConfiguration.get("aselect_application_domain");
            if(xAppDomain == null)
                log("aselect_application_domain was null -> activating auto recovery of domain");
            xAppPort = (String)xConfiguration.get("aselect_application_port");
            if(xAppPort == null)
                log("aselect_application_port was null -> activating auto recovery of port");
        }
        xAgentAddress = (String)xConfiguration.get("aselect_agent_address");
        if(xAgentAddress == null)
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_agent_address' must be specified");
        String xPort = (String)xConfiguration.get("aselect_agent_port");
        if(xPort == null)
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_agent_port' must be specified");
        try
        {
            xAgentPort = Integer.parseInt(xPort);
            if(xAgentPort < 1)
                throw new Exception("negative number not allowed");
        }
        catch(Exception e)
        {
            throw new ASelectException(xMethod + "Configuration parameter 'aselect_application_id' must be a  (positive) number");
        }
        log("Loading ASelect Servlet Filter successful");
    }

    public boolean authentication(HttpServletRequest xRequest, HttpServletResponse xResponse)
        throws ASelectException
    {
        String xMethod = "ASelectServletFilter.authentication -> ";
        try
        {
            if(verify_ticket(xRequest, xResponse))
                return true;
            if(verify_credentials(xRequest, xResponse))
                return false;
            if(authenticate_user(xRequest, xResponse))
                return false;
            else
                throw new Exception("The A-Select Authentication Module has received an unknown request. Please try again.");
        }
        catch(Exception e)
        {
            throw new ASelectException(xMethod + "Could not perform authentication : " + e.getMessage());
        }
    }

    public String getASelectUserId(HttpServletRequest xRequest)
    {
        String xUserId = getCookie(xRequest, "aselectuid");
        return doCGIDecode(xUserId);
    }

    public String getASelectOrganization(HttpServletRequest xRequest)
    {
        String xOrganization = getCookie(xRequest, "aselectorganization");
        return doCGIDecode(xOrganization);
    }

    public String getASelectSessionId(HttpServletRequest xRequest)
    {
        String xSessionId = getCookie(xRequest, "aselectticket");
        return doCGIDecode(xSessionId);
    }

    public void logout(HttpServletRequest xRequest, HttpServletResponse xResponse)
        throws ASelectException
    {
        String xTicket = null;
        String xUid = null;
        Hashtable xAgentResponse = null;
        String xResultCode = null;
        xUid = getASelectUserId(xRequest);
        xTicket = getASelectSessionId(xRequest);
        if(xTicket != null)
            try
            {
                xAgentResponse = transferRequest("request=kill_ticket&ticket=" + xTicket + "&app_id=" + xAppId);
            }
            catch(Exception exception) { }
    }

    private boolean verify_ticket(HttpServletRequest xRequest, HttpServletResponse xResponse)
        throws Exception
    {
        String xUid = null;
        String xOrganization = null;
        String xTicket = null;
        Hashtable xAgentResponse = null;
        String xResultCode = null;
        String xMethod = "ASelectServletFilter.verify_ticket -> ";
        xTicket = getASelectSessionId(xRequest);
        if(xTicket == null)
            return false;
        xUid = getASelectUserId(xRequest);
        if(xUid == null)
            return false;
        xOrganization = getASelectOrganization(xRequest);
        if(xOrganization == null)
            return false;
        try
        {
            xAgentResponse = transferRequest("request=verify_ticket&ticket=" + xTicket + "&app_id=" + xAppId + "&uid=" + xUid + "&organization=" + xOrganization);
        }
        catch(Exception e)
        {
            throw new Exception(xMethod + "The A-Select Agent could not be reached. (system code : " + e.getMessage() + "<br>  NOTE : The A-Select agent is not started or misconfigured<br>");
        }
        xResultCode = (String)xAgentResponse.get("result_code");
        return xResultCode != null && xResultCode.equals("0000");
    }

    private boolean verify_credentials(HttpServletRequest xRequest, HttpServletResponse xResponse)
        throws Exception
    {
        String xRID = null;
        String xCredentials = null;
        Hashtable xAgentResponse = null;
        String xResultCode = null;
        String xUid = null;
        String xOrganization = null;
        String xTicket = null;
        StringBuffer xRedirectUrl = null;
        String xMethod = "ASelectServletFilter.verify_credentials -> ";
        log(xMethod);
        xRID = xRequest.getParameter("rid");
        if(xRID == null)
            return false;
        xCredentials = xRequest.getParameter("aselect_credentials");
        if(xCredentials == null)
            return false;
        try
        {
            xAgentResponse = transferRequest("request=verify_credentials&rid=" + xRID + "&aselect_credentials=" + xCredentials);
        }
        catch(Exception e)
        {
            throw new Exception(xMethod + "The A-Select Agent could not be reached. (system code : " + e.getMessage() + "<br>  NOTE : The A-Select agent is not started or misconfigured<br>");
        }
        xResultCode = (String)xAgentResponse.get("result_code");
        if(xResultCode == null || !xResultCode.equals("0000"))
            throw new Exception(xMethod + "The A-Select Agent did return a malformed response.<br>Response from A-Select Agent : " + xAgentResponse + "<br>");
        xUid = (String)xAgentResponse.get("uid");
        if(xUid == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'uid'.<br>");
        xOrganization = (String)xAgentResponse.get("organization");
        if(xOrganization == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'organization'.<br>");
        xTicket = (String)xAgentResponse.get("ticket");
        if(xTicket == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'ticket'.<br>");
        xUid = doCGIEncode(xUid);
        Cookie xTicketCookie = new Cookie("aselectticket", xTicket);
        xTicketCookie.setPath("/");
        Cookie xUidCookie = new Cookie("aselectuid", xUid);
        xUidCookie.setPath("/");
        Cookie xOrgCookie = new Cookie("aselectorganization", xOrganization);
        xOrgCookie.setPath("/");
        xResponse.addCookie(xTicketCookie);
        xResponse.addCookie(xUidCookie);
        xResponse.addCookie(xOrgCookie);
        xRedirectUrl = xRequest.getRequestURL();
        String myQuery = xRequest.getQueryString();
        if(myQuery != null)
        {
            Hashtable myParams = Utils.convertCGIMessage(myQuery);
            myParams.remove("aselect_credentials");
            myParams.remove("rid");
            if(!myParams.isEmpty())
            {
                boolean xFirst = xRedirectUrl.indexOf("?")<0;
                String myKey;
                String myValue;
                for(Enumeration myKeys = myParams.keys(); myKeys.hasMoreElements(); xRedirectUrl.append(myKey + "=" + myValue))
                {
                    if(xFirst)
                        xRedirectUrl.append("?");
                    else
                        xRedirectUrl.append("&");
                    myKey = (String)myKeys.nextElement();
                    myValue = (String)myParams.get(myKey);
                    xFirst=false;
                }

            }
        }
        try
        {        	log("2-redirect " + xRedirectUrl.toString());

            xResponse.sendRedirect(xRedirectUrl.toString());
        }
        catch(Exception e)
        {
            throw new Exception(xMethod + "The system could not redirect to the following URL : " + xRedirectUrl + "<br>");
        }
        return true;
    }

    private boolean authenticate_user(HttpServletRequest xRequest, HttpServletResponse xResponse)
        throws Exception
    {
        String xParams = null;
        Hashtable xAgentResponse = null;
        String xResultCode = null;
        StringBuffer xRedirectUrl = null;
        String xAsUrl = null;
        String xASelectServer = null;
        String xRid = null;
        String xMethod = "ASelectServletFilter.authenticate_user -> ";
        log(xMethod);
        if(xAppUrl == null)
        {
            String tempProtocol = "";
            if(xAppProtocol == null)
                tempProtocol = xRequest.getScheme();
            else
                tempProtocol = xAppProtocol;
            dynamicUrl = tempProtocol + "://";
            if(xAppDomain == null)
                dynamicUrl += xRequest.getServerName();
            else
                dynamicUrl += xAppDomain;
            String tempPort = "";
            if(xAppPort == null)
                tempPort = tempPort + xRequest.getServerPort();
            else
                tempPort = tempPort + xAppPort;
            if(tempPort.equalsIgnoreCase("80") && tempProtocol.equalsIgnoreCase("http"))
                tempPort = "";
            else
            if(tempPort.equalsIgnoreCase("443") && tempProtocol.equalsIgnoreCase("https"))
                tempPort = "";
            else
                dynamicUrl += ":" + tempPort;
            dynamicUrl += xRequest.getRequestURI();
            xParams = xRequest.getQueryString();
            if(xParams != null)
                dynamicUrl += "?" + xParams;
        } else
        {
            dynamicUrl = xAppUrl;
        }
        log("dynamicUrl " + dynamicUrl);
        try
        {
            xAgentResponse = transferRequest("request=authenticate&app_url=" + dynamicUrl + "&app_id=" + xAppId);
        }
        catch(Exception e)
        {
            throw new Exception("The A-Select Agent could not be reached. (system code : " + e.getMessage() + "<br>  NOTE : The A-Select agent is not started or misconfigured<br>");
        }
        xResultCode = (String)xAgentResponse.get("result_code");
        if(xResultCode == null)
            throw new Exception(xMethod + "The A-Select Agent did return a malformed response.<br>Response from A-Select Agent : " + xAgentResponse + "<br>");
        if(!xResultCode.equals("0000"))
            if(xResultCode.equals("0102") || xResultCode.equals("0103"))
            {
                xRedirectUrl = xRequest.getRequestURL();
                try
                {
                log("3-redirect " + xRedirectUrl.toString());
                   xResponse.sendRedirect(xRedirectUrl.toString());
                }
                catch(Exception e)
                {
                    throw new Exception(xMethod + "The system could not redirect to the following URL : " + xRedirectUrl + "<br>");
                }
            } else
            {
                throw new Exception(xMethod + "The A-Select Agent did return an error : " + xResultCode + "<br>");
            }
        xRid = (String)xAgentResponse.get("rid");
        if(xRid == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'rid'.<br>");
        xAsUrl = (String)xAgentResponse.get("as_url");
        if(xAsUrl == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'as_url'.<br>");
        xASelectServer = (String)xAgentResponse.get("a-select-server");
        if(xASelectServer == null)
            throw new Exception(xMethod + "The A-Select Agent did return a mallformed response :<br>missinge param 'a-select-server'.<br>");
 // xAsUrl bevat %XX strings, decode WIM
        xAsUrl = URLDecoder.decode(xAsUrl, "UTF-8");
        xRedirectUrl = new StringBuffer(xAsUrl);
        xRedirectUrl.append("&rid=");
        xRedirectUrl.append(xRid);
        xRedirectUrl.append("&a-select-server=");
        xRedirectUrl.append(xASelectServer);
        try
        {
        	log("1-redirect " + xRedirectUrl.toString());
            xResponse.sendRedirect(xRedirectUrl.toString());
        }
        catch(Exception e)
        {
            throw new Exception(xMethod + "The system could not redirect to the following URL : " + xRedirectUrl + "<br>");
        }
        return true;
    }

    private Hashtable transferRequest(String xRequest)
        throws Exception
    {
        Socket xSocket = null;
        PrintStream xOut = null;
        BufferedReader xIn = null;
        String xAgentResponse = null;
        xSocket = new Socket(xAgentAddress, xAgentPort);
        xOut = new PrintStream(xSocket.getOutputStream());
        xIn = new BufferedReader(new InputStreamReader(xSocket.getInputStream()));
        xOut.println(xRequest);
        xAgentResponse = xIn.readLine();
        return Utils.convertCGIMessage(xAgentResponse);
    }

    // wim make public, handy
    public  String getCookie(HttpServletRequest xRequest, String xCookieName)
    {
        Cookie xCookies[] = (Cookie[])null;
        xCookies = xRequest.getCookies();
        if(xCookies == null)
            return null;
        for(int i = 0; i < xCookies.length; i++)
            if(xCookies[i].getName().equals(xCookieName))
                return xCookies[i].getValue();

        return null;
    }

    private void log(String s)
    {
        try
        {
            if(xUseLogging)
            {
                StringBuffer xString = new StringBuffer();
                xString.append("[");
                xString.append((new Date()).toString());
                xString.append("], ");
                xString.append(s);
                xString.append("\n");
                RandomAccessFile xFile = new RandomAccessFile(xAuthenticationLogFile, "rw");
                xFile.seek(xFile.length());
                xFile.writeBytes(xString.toString());
                xFile.close();
            } else
            {
                System.out.println(s);
            }
        }
        catch(Exception e)
        {
            System.out.println(s);
        }
    }

    private String doCGIEncode(String xValue)
    {
        String xEncoded = null;
        if(xValue != null)
            xEncoded = xValue.replace(' ', '+');
        return xEncoded;
    }

    private String doCGIDecode(String xValue)
    {
        String xDecoded = null;
        if(xValue != null)
        {
            xDecoded = xValue.replace('+', ' ');
            for(int iPos = xDecoded.indexOf("%2B"); iPos != -1; iPos = xDecoded.indexOf("%2B"))
                xDecoded = xDecoded.substring(0, iPos) + " " + xDecoded.substring(iPos + 3);

        }
        return xDecoded;
    }

    private String doSpecialConvert(String xValue)
    {
        String xConverted = null;
        if(xValue != null)
        {
            xConverted = xValue;
            for(int iPos = xConverted.indexOf("+"); iPos != -1; iPos = xConverted.indexOf("+"))
                xConverted = xConverted.substring(0, iPos) + "%2B" + xConverted.substring(iPos + 1);

        }
        return xConverted;
    }

    private String xAppId;
    String dynamicUrl;
    private String xAppUrl;
    private String xAppProtocol;
    private String xAppDomain;
    private String xAppPort;
    private String xAgentAddress;
    private int xAgentPort;
    private String xAuthenticationLogFile;
    private boolean xUseLogging;
}
