package mvndemo;

import iBoxDB.LocalServer.*;

public class Demo {

    public static void main(String[] args) {
        DB.root("/tmp/");
        System.out.println(example.JDB.run(true));
    }
}
