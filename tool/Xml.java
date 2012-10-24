package tool;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple XML Builder
 * @author nakazawaken1
 */
public class Xml {
    /**
     * 要素（タグ）名
     */
    String name;
    /**
     * 属性の名前と値の連想配列
     */
    Map<String, Object> attributes;
    /**
     * 子要素の配列
     */
    List<Xml> children;
    /**
     * 要素内のテキスト
     */
    Object text;
    /**
     * 親要素
     */
    public Xml parent;
    /**
     * インデント空白数
     */
    public static int indent = 2;
    /**
     * リストとして使用する型
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends List<Xml>> list = (Class<? extends List<Xml>>) ArrayList.class;
    /**
     * マップとして使用する型
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Map<String, Object>> map = (Class<? extends Map<String, Object>>) HashMap.class;
    /**
     * 要素内テキストのエスケープ対象文字と変換後文字の連想配列(&は除く)
     */
    public static Map<String, String> escapeAtText = new HashMap<String, String>();
    /**
     * 属性値のエスケープ対象文字と変換後文字の連想配列(&は除く)
     */
    public static Map<String, String> escapeAtAttr = new HashMap<String, String>();
    static {
        escapeAtText.put("<", "&lt;");
        escapeAtText.put(">", "&gt;");
        escapeAtAttr.put("\"", "&quot;");
    }

    /**
     * 連想配列のキーを値に置換
     * @param map 置換するキーと値の連想配列
     * @param text 対象テキスト
     * @return 置換後のテキスト
     */
    public static String replace(Map<String, String> map, String text) {
        text = text.replace("&", "&amp;");
        for (Map.Entry<String, String> i : map.entrySet()) {
            text = text.replace(i.getKey(), i.getValue());
        }
        return text;
    }

    /**
     * 要素作成
     * @param name 要素名
     * @param text 要素内のテキスト
     */
    public Xml(String name, Object text) {
        this.name = name;
        this.text = text;
    }

    /**
     * 要素作成
     * @param name 要素名
     */
    public Xml(String name) {
        this(name, null);
    }

    /**
     * 子要素追加
     * @param name 要素名
     * @param text 要素内のテキスト
     * @return 追加した子要素
     */
    public Xml child(String name, Object text) {
        if (this.children == null) {
            try {
                this.children = list.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        Xml node = new Xml(name, text);
        node.parent = this;
        this.children.add(node);
        return node;
    }

    /**
     * 子要素追加
     * @param name 要素名
     * @return 追加した子要素
     */
    public Xml child(String name) {
        return child(name, null);
    }

    /**
     * 属性追加（すでに存在する属性名の場合は値を上書き）
     * @param name 属性名
     * @param value 属性値
     * @return
     */
    public Xml attr(String name, Object value) {
        if (this.attributes == null) {
            try {
                this.attributes = map.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        this.attributes.put(name, value);
        return this;
    }

    /**
     * 兄弟要素追加
     * @param name 要素名
     * @param text 要素内のテキスト
     * @return 追加した兄弟要素
     */
    public Xml next(String name, Object text) {
        if (this.parent == null) {
            return new Xml(name, text);
        }
        return this.parent.child(name, text);
    }

    /**
     * 兄弟要素追加
     * @param name 要素名
     * @return 追加した兄弟要素
     */
    public Xml next(String name) {
        return next(name, null);
    }

    /**
     * XML文字列化
     * @param indent 行頭空白数
     * @return XML文字列
     */
    public String toString(int indent) {
        StringBuffer xml = new StringBuffer();
        String text = null;
        if (this.text != null) {
            text = this.text.toString();
        }
        boolean empty = (this.children == null || this.children.size() <= 0) && (text == null || text.length() <= 0);
        if (indent != 0) {
            xml.append('\n');
        }
        for (int i = 0; i < indent; i++) {
            xml.append(' ');
        }
        xml.append('<').append(this.name);
        if (this.attributes != null) {
            for (Map.Entry<String, Object> i : this.attributes.entrySet()) {
                xml.append(' ').append(i.getKey()).append("=\"").append(replace(escapeAtAttr, i.getValue().toString())).append("\"");
            }
        }
        if (empty) {
            xml.append("/>");
        } else {
            xml.append('>');
            if (this.children != null) {
                for (Xml i : this.children) {
                    xml.append(i.toString(indent + Xml.indent));
                }
                xml.append('\n');
            }
            if (text != null) {
                xml.append(replace(escapeAtText, text));
            } else {
                for (int i = 0; i < indent; i++) {
                    xml.append(' ');
                }
            }
            xml.append("</").append(name).append('>');
        }
        return xml.toString();
    }

    /**
     * XML文字列化
     * @return XML文字列
     */
    public String toString() {
        return toString(0);
    }

    /**
     * ルート要素を取得
     * @return
     */
    public Xml root() {
        Xml node = this;
        while (node.parent != null) {
            node = node.parent;
        }
        return node;
    }

    /**
     * XMLヘッダ生成
     * @param version バージョン
     * @param encode 文字コード
     * @return XMLヘッダ
     */
    public static String header(String version, String encode) {
        return "<?xml version=\"" + version + "\" encoding=\"" + encode + "\"?>\n";
    }

    /**
     * ヘッダつきでXML文字列化
     * @param version バージョン
     * @param encode 文字コード
     * @param doctype DOCTYPE（ルート要素名より後から>の前までに記述する内容を指定)
     * @return XML文字列
     */
    public String withHeader(String version, String encode, String doctype) {
        String result = header(version, encode);
        Xml root = this.root();
        if(doctype != null) {
            result += "<!DOCTYPE " + root.name + " " + doctype + ">\n";
        }
        return result + root;
    }

    /**
     * ヘッダつきでXML文字列化
     * @return XML文字列
     */
    public String withHeader() {
        return withHeader("1.0", "UTF-8", null);
    }

    /**
     * オブジェクトをXML文字列化
     * @param o オブジェクト
     * @return XML文字列
     */
    public static String toString(Object o) {
        Class<?> c = o.getClass();
        Xml xml = new Xml(c.getSimpleName());
        Xml node = null;
        for(Field field : c.getFields()) {
            try {
                if(node == null) {
                    node = xml.child(field.getName(), field.get(o));
                } else {
                    node.next(field.getName(), field.get(o));
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return xml.toString();
    }

    /**
     * オブジェクトをヘッダつきでXML文字列化
     * @param o オブジェクト
     * @param version バージョン
     * @param encode 文字コード
     * @return XML文字列
     */
    public static String withHeader(Object o, String version, String encode) {
        return header(version, encode) + toString(o);
    }

    /**
     * 使用例
     * @param args
     */
    public static void main(String[] args) {
        // 単一要素
        System.out.println(new Xml("root").withHeader());
        System.out.println();

        // テキストあり
        System.out.println(new Xml("root", "単一要素").withHeader());
        System.out.println();

        // 属性あり
        System.out.println(new Xml("root", "単一要素＋属性").attr("id", 1234).withHeader());
        System.out.println();

        // 子要素あり
        Xml node = new Xml("root").attr("title", "子要素あり").child("br").next("br").attr("style", "clear:both");
        int id = 1;
        for (String value : new String[] { "", "def", "ghi" }) {
            node.next("item", value).attr("id", id);
            id++;
        }
        Xml.indent = 1;
        System.out.println(node.withHeader());
        System.out.println();

        // エスケープ文字の処理
        System.out.println(new Xml("root", "<エスケープ文字>の処理&").attr("id", "12\"34&").withHeader());
        System.out.println();

        //オブジェクトをXML化
        class Clas {
            @SuppressWarnings("unused")
            public int id = 123;
            @SuppressWarnings("unused")
            public String name = "オブジェクトをXML化";
            @SuppressWarnings("unused")
            public Date date = new Date();
        }
        System.out.println(Xml.toString(new Clas()));
    }
}
