package pb.r1lit.LogicMenu.gui.model;

import java.util.List;

public class MenuRequirementGroup {
    private final List<MenuRequirement> requirements;
    private final int minimum;
    private final List<MenuAction> denyActions;

    public MenuRequirementGroup(List<MenuRequirement> requirements, int minimum, List<MenuAction> denyActions) {
        this.requirements = requirements;
        this.minimum = minimum;
        this.denyActions = denyActions;
    }

    public List<MenuRequirement> getRequirements() {
        return requirements;
    }

    public int getMinimum() {
        return minimum;
    }

    public List<MenuAction> getDenyActions() {
        return denyActions;
    }
}

