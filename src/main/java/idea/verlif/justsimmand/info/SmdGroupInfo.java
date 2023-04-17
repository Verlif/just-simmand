package idea.verlif.justsimmand.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 指令信息
 */
public class SmdGroupInfo {

    /**
     * 指令组key
     */
    private String key;

    /**
     * 指令组描述
     */
    private String description;

    /**
     * 指令方法集
     */
    private final List<SmdMethodInfo> smdMethodInfoList;

    public SmdGroupInfo() {
        this.smdMethodInfoList = new ArrayList<>();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SmdMethodInfo> getMethodInfoList() {
        return smdMethodInfoList;
    }

    public void addSmdMethodInfo(SmdMethodInfo smdMethodInfo) {
        smdMethodInfoList.add(smdMethodInfo);
    }

    public void addSmdMethodInfo(Collection<SmdMethodInfo> collection) {
        smdMethodInfoList.addAll(collection);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(key).append(" - ").append(description != null ? description : "暂无描述").append("\n");
        // 遍历指令组的所有方法
        for (SmdMethodInfo methodInfo : smdMethodInfoList) {
            String keys = Arrays.toString(methodInfo.getKey());
            // 指令方法描述
            str.append("\t[ ").append(keys, 1, keys.length() - 1).append(" ]")
                    .append(" --> ").append(methodInfo.getDescription() != null ? methodInfo.getDescription() : "暂无描述").append("\n");
            // 显示指令示例
            if (methodInfo.getExample() != null && methodInfo.getExample().length() > 0) {
                str.append("\t--> ").append(methodInfo.getExample()).append("\n");
            }
            if (!methodInfo.getArgInfoList().isEmpty()) {
                // 遍历指令方法参数
                for (SmdArgInfo smdArgInfo : methodInfo.getArgInfoList()) {
                    String argName = smdArgInfo.getKey();
                    // 强制需要的参数显示为 *arg* , 否则显示为 [arg]
                    if (smdArgInfo.isForce()) {
                        argName = "*" + argName + "*";
                    } else {
                        argName = "[" + argName + "]";
                    }
                    str.append("\t\t").append(fillSpace(20, argName)).append("\t");
                    // 显示默认值
                    if (smdArgInfo.getDefaultVal() == null) {
                        str.append(fillSpace(20, "_NULL_")).append("\t");
                    } else {
                        str.append(fillSpace(20, smdArgInfo.getDefaultVal())).append("\t");
                    }
                    if (smdArgInfo.getDescription() == null) {
                        str.append("\n");
                    } else {
                        str.append(smdArgInfo.getDescription()).append("\n");
                    }
                }
            }
        }
        return str.toString();
    }

    private String fillSpace(int total, String str) {
        StringBuilder strb = new StringBuilder(str);
        for (int i = str.length(); i < total; i++) {
            strb.append(" ");
        }
        return strb.toString();
    }
}
