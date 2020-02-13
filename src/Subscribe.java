import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Author: zero
 * @Date: 2020/2/4 23:52
 */
public class Subscribe {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("请输入网址或ssr链接或文件路径");
            return;
        }
        ArrayList<String> ssrLink = handle(args[0]);


        String path = System.getProperty("user.dir");
        new File(path + File.separator + "config").mkdirs();
        System.out.println("创建目录 "+path + File.separator + "config");
        for (int i = 0; i < ssrLink.size(); i++) {
            HashMap<String, String> ssrMap = decoderSsrLink(ssrLink.get(i));
            System.out.println("写入节点 "+ssrMap.get("server"));
            String content = mapToJson(ssrMap);
            write(path + File.separator + "config" + File.separator + "config.json" + i, content);
        }
        System.out.println("完成，请到 "+path + File.separator + "config"+" 查看");
    }


    private static ArrayList<String> handle(String args) throws IOException {
        ArrayList<String> ssrLink = new ArrayList<>();
        //网页
        if ("http".equalsIgnoreCase(args.substring(0, 4))) {
            System.out.println("连接网页中.........");
             ssrLink = getWeb(args);
            System.out.println("解析网页完成，一共有"+ssrLink.size()+"个节点");
        }
        if ("ssr".equalsIgnoreCase(args.substring(0, 3))) {
            ssrLink.add(args.replace("ssr://", ""));
        }
        if (new File(args).exists()) {
            System.out.println("读取文件中.........");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(args)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!"".equals(line)) {
                    ssrLink.add(line.replace("ssr://", ""));
                }
            }
            System.out.println("读取文件中完成，一共有"+ssrLink.size()+"个节点");
        }


        return ssrLink;
    }



    /**
     * 输入 订阅地址
     * 输出 xxxx;xxxx
     *
     * @param webUrl 订阅网址
     * @return base64加密的ssr短链接
     */
    private static ArrayList<String> getWeb(String webUrl) {
        ArrayList<String> ssrLink = new ArrayList<>();
        //读取网页
        try {
            InputStream inputStream = new URL(webUrl).openStream();
            String s = new String(inputStream.readAllBytes());
            //解密网页
            String source = new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8).replace("ssr://", "");
            ssrLink.addAll(Arrays.asList(source.split("\n")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ssrLink;
    }

    /**
     * 解密订阅地址输出的内容
     *
     * @param ssrLink 单个ssr连接
     * @return 包含ssr内容的map
     */
    private static HashMap<String, String> decoderSsrLink(String ssrLink) {
        ArrayList<String> ssrFinal = new ArrayList<>();

        String ssrRaw = new String(org.apache.commons.codec.binary.Base64.decodeBase64(ssrLink), StandardCharsets.UTF_8);

//        System.out.println(ssrRaw);
        //hk2.xxx.xyz:7324:auth_chain_a:chacha20:tls1.2_ticket_auth:xxxx/?obfsparam=&protoparam=&remarks=xxxx
        String[] firstSplit = ssrRaw.split("/\\?");
        //firstSplit[0]   hk2.xxx.xyz:7324:auth_chain_a:chacha20:tls1.2_ticket_auth:xxxx
        //firstSplit[1]   obfsparam=&protoparam=&remarks=xxxx&group=xxxx

        //firstSplit[0]
        ssrFinal.addAll(Arrays.asList(firstSplit[0].split(":")));
        ssrFinal.set(ssrFinal.size() - 1, new String(org.apache.commons.codec.binary.Base64.decodeBase64(ssrFinal.get(ssrFinal.size() - 1)),
                StandardCharsets.UTF_8));
        /**
         * us2.xxx.net
         * 888
         * auth_chain_a
         * none
         * tls1.2_ticket_auth
         * 1xxx
         */
        HashMap<String, String> ssrMap = new HashMap<>(16);
        ssrMap.put("server", ssrFinal.get(0));
        try {
            ssrMap.put("server_port", ssrFinal.get(1));
        } catch (Exception e) {
            System.out.println("链接解析失败，连接内容：" + ssrLink);
            System.out.println("解析结果："+ssrRaw);
            System.exit(0);
        }

        ssrMap.put("protocol", ssrFinal.get(2));
        ssrMap.put("method", ssrFinal.get(3));
        ssrMap.put("obfs", ssrFinal.get(4));
        ssrMap.put("password", ssrFinal.get(5));


        // firstSplit[1]  obfsparam=xxx&protoparam=&remarks=xxxx&group=xxxx
        String other =
                firstSplit[1].replace("obfsparam=", "").replace("protoparam=", "").replace("remarks=", "").replace("group=", "");

        String[] param = other.split("&");
        for (String s : param) {
            ssrFinal.add(new String(org.apache.commons.codec.binary.Base64.decodeBase64(s)));
        }
        ssrMap.put("obfsparam", ssrFinal.get(6));
        ssrMap.put("protoparam", ssrFinal.get(7));
        ssrMap.put("remarks", ssrFinal.get(8));
        try {
            ssrMap.put("group", ssrFinal.get(9));
        } catch (Exception ignored) {
        }

        return ssrMap;
    }

    /**
     * 介入ssr配置文件
     *
     * @param path    ssr配置文件的位置
     * @param context 要写入的内容
     */
    private static void write(String path, String context) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(context);
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将map转为json
     *
     * @param ssrMap
     * @return
     */
    private static String mapToJson(HashMap<String, String> ssrMap) {

        String out1 = "{\n" +
                "    \"server\": \"" + ssrMap.get("server") + "\",\n";
        String out2 = "    \"server_ipv6\": \"::\",\n" +
                "    \"server_port\": " + ssrMap.get("server_port") + ",\n" +
                "    \"local_address\": \"0.0.0.0\",\n" +
                "    \"local_port\": 1080,\n" +
                "\n";
        String out3 = "    \"password\": \"" + ssrMap.get("password") + "\",\n" +
                "    \"method\": \"" + ssrMap.get("method") + "\",\n" +
                "    \"protocol\": \"" + ssrMap.get("protocol") + "\",\n" +
                "    \"protocol_param\": \"" + ssrMap.get("protoparam") + "\",\n" +
                "    \"obfs\": \"" + ssrMap.get("obfs") + "\",\n" +
                "    \"obfs_param\": \"" + ssrMap.get("obfsparam") + "\",\n" +
                "    \"speed_limit_per_con\": 0,\n" +
                "    \"speed_limit_per_user\": 0,\n";
        String out4 = "\n" +
                "    \"additional_ports\" : {},\n" +
                "    \"additional_ports_only\" : false,\n" +
                "    \"timeout\": 120,\n" +
                "    \"udp_timeout\": 60,\n" +
                "    \"dns_ipv6\": false,\n" +
                "    \"connect_verbose_info\": 0,\n" +
                "    \"redirect\": \"\",\n" +
                "    \"fast_open\": false\n" +
                "}";

        return out1 + out2 + out3 + out4;

    }

}
