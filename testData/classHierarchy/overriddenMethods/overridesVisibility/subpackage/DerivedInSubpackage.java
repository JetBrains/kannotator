package classHierarchy.overriddenMethods.overridesVisibility.subpackage;

import classHierarchy.overriddenMethods.overridesVisibility.Base;

public class DerivedInSubpackage extends Base {

    public void publicFun() {}

    protected void protectedFun() {}

    void packageFun() {}

    private void privateFun() {}

}
