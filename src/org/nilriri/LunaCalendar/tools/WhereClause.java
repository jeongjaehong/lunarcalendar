package org.nilriri.LunaCalendar.tools;


public class WhereClause {

    private StringBuilder clause;
    private String param = "";
    private Boolean useParam = new Boolean(false);

    public WhereClause() {
        this.useParam = false;
        clause = new StringBuilder("1=1");
    }

    public WhereClause(boolean useParam) {
        this.useParam = useParam;
        clause = new StringBuilder("1=1");
    }

    public void addParam(Object val) {
        if ("".equals(this.param)) {
            this.param = (String) val;
        } else {
            this.param += "," + (String) val;
        }
    }

    public void put(String key, Object value) {
        putValue(key, value, "=");
    }

    public void put(String key, Object value, String op) {
        if ("".equals(op) || op == null) {
            putValue(key, value, "=");
        } else {
            putValue(key, value, op);
        }
    }

    private void putValue(String key, Object value, String op) {
        if (value == null || "".equals(value)) {
        } else {
            if (useParam) {
                addParam(value + "");

                clause.append(" and ").append(key).append(" " + op + " ").append(" ? ");

            } else {
                if (value.getClass() == String.class) {
                    putString(key, (String) value, op);
                } else if (value.getClass() == Byte.class || value.getClass() == Short.class || value.getClass() == Integer.class || value.getClass() == Long.class || value.getClass() == Double.class || value.getClass() == Float.class) {
                    putNumeric(key, (String) value, op);
                } else if (value.getClass() == Boolean.class) {
                    putString(key, ((Boolean) value ? "(1=1)" : "(1=2)"), op);
                } else {
                    putString(key, (String) value, op);
                }
            }
        }
    }

    private void putString(String key, String value, String op) {
        if (value != null && !"".equals(value)) {
            clause.append(" and ").append(key).append(" " + op + " ");
            clause.append("'").append(value).append("'");
        }
    }

    private void putNumeric(String key, String value, String op) {
        if (value != null) {
            clause.append(" and ").append(key).append(" " + op + " ");
            clause.append(value);
        }
    }

    public void putNull(String key) {
        putNull(key, null);
    }

    public void putNull(String key, Object value) {
        if (value == null) {
            clause.append(" and ").append(key).append(" is null ");
        } else if ("".equals(value)) {
            clause.append(" and ").append(key).append(" = '' ");
        }
    }

    public String getClause() {
        return clause.toString();
    }

    public String[] getParam() {
        return Common.tokenFn(this.param, ",");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("WhereClause={");
        result.append("Caluse=").append(this.clause.toString()).append(", ");
        result.append("Parameter=").append(this.param);
        result.append("}");
        return result.toString();
    }
}