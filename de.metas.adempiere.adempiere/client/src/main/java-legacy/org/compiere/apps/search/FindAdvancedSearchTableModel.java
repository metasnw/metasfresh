package org.compiere.apps.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.adempiere.util.Services;
import org.adempiere.util.api.IMsgBL;
import org.compiere.apps.search.FindAdvancedSearchTableModelRow.Join;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;

/**
 * Advanced search table model.
 * 
 * @author metas-dev <dev@metas-fresh.com>
 */
class FindAdvancedSearchTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = -4525032877936896927L;

	// services
	private final transient IMsgBL msgBL = Services.get(IMsgBL.class);

	/** Index Join Operator = 0 */
	static final int INDEX_JOIN = 0;
	/** Index ColumnName = 1 */
	static final int INDEX_COLUMNNAME = 1;
	/** Index Operator = 2 */
	static final int INDEX_OPERATOR = 2;
	/** Index Value = 3 */
	static final int INDEX_VALUE = 3;
	/** Index Value2 = 4 */
	static final int INDEX_VALUE2 = 4;
	//
	private static final int COLUMNS_COUNT = 5;

	private final List<FindAdvancedSearchTableModelRow> rows = new ArrayList<>();

	private Collection<FindPanelSearchField> availableSearchFields = null;

	public FindAdvancedSearchTableModel()
	{
		super();
	}

	public void setAvailableSearchFields(final Collection<FindPanelSearchField> availableSearchFields)
	{
		this.availableSearchFields = availableSearchFields;
	}

	public Collection<FindPanelSearchField> getAvailableSearchFields()
	{
		return availableSearchFields;
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return COLUMNS_COUNT;
	}

	@Override
	public String getColumnName(final int columnIndex)
	{
		if (INDEX_JOIN == columnIndex)
		{
			return msgBL.translate(Env.getCtx(), "JoinElement");
		}
		else if (INDEX_COLUMNNAME == columnIndex)
		{
			return msgBL.translate(Env.getCtx(), "AD_Column_ID");

		}
		else if (INDEX_OPERATOR == columnIndex)
		{
			return msgBL.translate(Env.getCtx(), "Operator");
		}
		else if (INDEX_VALUE == columnIndex)
		{
			return msgBL.translate(Env.getCtx(), "QueryValue");
		}
		else if (INDEX_VALUE2 == columnIndex)
		{
			return msgBL.translate(Env.getCtx(), "QueryValue2");
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		}
	}

	public final FindAdvancedSearchTableModelRow getRow(final int rowIndex)
	{
		return rows.get(rowIndex);
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		final FindAdvancedSearchTableModelRow row = getRow(rowIndex);
		if (INDEX_JOIN == columnIndex)
		{
			return row.getJoin();
		}
		else if (INDEX_COLUMNNAME == columnIndex)
		{
			return row.getSearchField();
		}
		else if (INDEX_OPERATOR == columnIndex)
		{
			return row.getOperator();
		}
		else if (INDEX_VALUE == columnIndex)
		{
			return row.getValue();
		}
		else if (INDEX_VALUE2 == columnIndex)
		{
			return row.getValueTo();
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		}
	}

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex)
	{
		final FindAdvancedSearchTableModelRow row = getRow(rowIndex);
		if (INDEX_JOIN == columnIndex)
		{
			return rowIndex > 0;
		}
		else if (INDEX_COLUMNNAME == columnIndex)
		{
			return true;
		}
		else if (INDEX_OPERATOR == columnIndex)
		{
			return row.getSearchField() != null;
		}
		else if (INDEX_VALUE == columnIndex)
		{
			return row.getSearchField() != null;
		}
		else if (INDEX_VALUE2 == columnIndex)
		{
			// TODO: and operator is BETWEEN
			return row.getSearchField() != null;
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		}
	}

	@Override
	public void setValueAt(final Object value, final int rowIndex, final int columnIndex)
	{
		final FindAdvancedSearchTableModelRow row = getRow(rowIndex);
		if (INDEX_JOIN == columnIndex)
		{
			final Join join = (Join)value;
			row.setJoin(join);
		}
		else if (INDEX_COLUMNNAME == columnIndex)
		{
			final FindPanelSearchField searchField = (FindPanelSearchField)value;
			row.setSearchField(searchField);
		}
		else if (INDEX_OPERATOR == columnIndex)
		{
			final ValueNamePair operator = (ValueNamePair)value;
			row.setOperator(operator);
		}
		else if (INDEX_VALUE == columnIndex)
		{
			row.setValue(value);
		}
		else if (INDEX_VALUE2 == columnIndex)
		{
			row.setValueTo(value);
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index: " + columnIndex);
		}
	}

	public final void clear()
	{
		if (rows.isEmpty())
		{
			return;
		}

		final int lastRow = rows.size() - 1;
		rows.clear();

		fireTableRowsDeleted(0, lastRow);
	}

	public void setRows(final List<FindAdvancedSearchTableModelRow> rowsToSet)
	{
		rows.clear();

		if (rowsToSet != null)
		{
			rows.addAll(rowsToSet);
		}

		fireTableDataChanged();
	}

	public List<FindAdvancedSearchTableModelRow> getRows()
	{
		return new ArrayList<>(rows);
	}

	private final void addRow(final FindAdvancedSearchTableModelRow rowToAdd)
	{
		rows.add(rowToAdd);

		final int rowIndex = rows.size() - 1;
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	public FindAdvancedSearchTableModelRow newRow()
	{
		final FindAdvancedSearchTableModelRow row = new FindAdvancedSearchTableModelRow();
		addRow(row);
		return row;
	}

	public void removeRow(final int rowIndex)
	{
		rows.remove(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);

	}
}
