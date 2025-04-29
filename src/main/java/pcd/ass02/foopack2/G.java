package pcd.ass02.foopack2;

public class G {
    public G() {
    }

    public void method() {
        var g = new innerG();
        g.innerMethod();
    }

    public class innerG {
        public void innerMethod() {}
    }
}
