package net.chakmeshma.brutengine.rendering;


public interface VariableReferenceable {
    String getTypeName();

    String getName();

    Class getValueType();

    int getValuesCount();

    //region inner classes
    interface VariableMatcher {
        boolean matches(VariableReferenceable variable);

        final class EqualityMatcher implements VariableMatcher {
            private String declaredDefinedTypeName;
            private String declaredDefinedName;

            public EqualityMatcher(String declaredDefinedTypeName, String declaredDefinedName) {
                this.declaredDefinedName = declaredDefinedName;
                this.declaredDefinedTypeName = declaredDefinedTypeName;
            }

            @Override
            public boolean matches(VariableReferenceable variable) {
                return (variable.getName().equals(this.declaredDefinedName) && variable.getTypeName().equals(this.declaredDefinedTypeName));
            }
        }
    }
    //endregion
}
