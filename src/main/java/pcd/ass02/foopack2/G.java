package pcd.ass02.foopack2;

public class G {
    public G() {
    }

    public void method() {
        var g = new InnerG();
        g.innerMethod();
    }

    public class InnerG {
        public void innerMethod() {}
    }
}
