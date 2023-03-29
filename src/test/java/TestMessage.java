import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.MPCException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Arrays;

public class TestMessage {

    public static void main(String[] args) throws MPCException {
        try (Context ctx = Context.initGenerateEcdsaKey(1)) {

            System.out.println("context.info:");
            System.out.println(ToStringBuilder.reflectionToString(ctx.getInfo(), ToStringStyle.JSON_STYLE));

            System.out.println("MessageAndFlags:");
            Context.MessageAndFlags mf = ctx.step(null);
            System.out.println(ToStringBuilder.reflectionToString(mf, ToStringStyle.JSON_STYLE));

            byte[] msgBuf = null;
            msgBuf = mf.message.toBuf();

            System.out.println("message:");
            System.out.println(ToStringBuilder.reflectionToString(mf.message, ToStringStyle.JSON_STYLE));
            System.out.println(Arrays.toString(mf.message.toBuf()));

            System.out.println("message.info:");
            System.out.println(ToStringBuilder.reflectionToString(mf.message.getInfo(), ToStringStyle.JSON_STYLE));

            System.out.println("context.info:");
            System.out.println(ToStringBuilder.reflectionToString(ctx.getInfo(), ToStringStyle.JSON_STYLE));

        }
    }

}
