/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package de.metas.printing.model;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for AD_Print_Clients
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_AD_Print_Clients extends org.compiere.model.PO implements I_AD_Print_Clients, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 746272087L;

    /** Standard Constructor */
    public X_AD_Print_Clients (Properties ctx, int AD_Print_Clients_ID, String trxName)
    {
      super (ctx, AD_Print_Clients_ID, trxName);
      /** if (AD_Print_Clients_ID == 0)
        {
			setAD_Print_Clients_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_Print_Clients (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }


    /** Load Meta Data */
    @Override
    protected org.compiere.model.POInfo initPO (Properties ctx)
    {
      org.compiere.model.POInfo poi = org.compiere.model.POInfo.getPOInfo (ctx, Table_Name, get_TrxName());
      return poi;
    }

	/** Set AD_Print_Clients.
		@param AD_Print_Clients_ID AD_Print_Clients	  */
	@Override
	public void setAD_Print_Clients_ID (int AD_Print_Clients_ID)
	{
		if (AD_Print_Clients_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Print_Clients_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Print_Clients_ID, Integer.valueOf(AD_Print_Clients_ID));
	}

	/** Get AD_Print_Clients.
		@return AD_Print_Clients	  */
	@Override
	public int getAD_Print_Clients_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Print_Clients_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_Session getAD_Session() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_Session_ID, org.compiere.model.I_AD_Session.class);
	}

	@Override
	public void setAD_Session(org.compiere.model.I_AD_Session AD_Session)
	{
		set_ValueFromPO(COLUMNNAME_AD_Session_ID, org.compiere.model.I_AD_Session.class, AD_Session);
	}

	/** Set Nutzersitzung.
		@param AD_Session_ID 
		User Session Online or Web
	  */
	@Override
	public void setAD_Session_ID (int AD_Session_ID)
	{
		if (AD_Session_ID < 1) 
			set_Value (COLUMNNAME_AD_Session_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Session_ID, Integer.valueOf(AD_Session_ID));
	}

	/** Get Nutzersitzung.
		@return User Session Online or Web
	  */
	@Override
	public int getAD_Session_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Session_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Letzter Kontakt.
		@param DateLastPoll Letzter Kontakt	  */
	@Override
	public void setDateLastPoll (java.sql.Timestamp DateLastPoll)
	{
		set_Value (COLUMNNAME_DateLastPoll, DateLastPoll);
	}

	/** Get Letzter Kontakt.
		@return Letzter Kontakt	  */
	@Override
	public java.sql.Timestamp getDateLastPoll () 
	{
		return (java.sql.Timestamp)get_Value(COLUMNNAME_DateLastPoll);
	}

	/** Set Host key.
		@param HostKey 
		Unique identifier of a host
	  */
	@Override
	public void setHostKey (java.lang.String HostKey)
	{
		set_Value (COLUMNNAME_HostKey, HostKey);
	}

	/** Get Host key.
		@return Unique identifier of a host
	  */
	@Override
	public java.lang.String getHostKey () 
	{
		return (java.lang.String)get_Value(COLUMNNAME_HostKey);
	}
}