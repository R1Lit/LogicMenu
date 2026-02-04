package pb.r1lit.LogicMenu.gui.model;

import java.util.Map;

public class MenuState {
    private final String menuId;
    private final int page;
    private final Map<String, String> customVars;

    public MenuState(String menuId, int page, Map<String, String> customVars) {
        this.menuId = menuId;
        this.page = page;
        this.customVars = customVars;
    }

    public String getMenuId() {
        return menuId;
    }

    public int getPage() {
        return page;
    }

    public Map<String, String> getCustomVars() {
        return customVars;
    }
}

