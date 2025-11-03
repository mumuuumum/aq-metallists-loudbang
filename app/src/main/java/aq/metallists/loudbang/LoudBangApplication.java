package aq.metallists.loudbang;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

public class LoudBangApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder();
        builder.setBuildConfigClass(BuildConfig.class);
        builder.setReportFormat(StringFormat.JSON);
        builder.withPluginConfigurations(
                new DialogConfigurationBuilder()
                        .withText(base.getString(R.string.acra_sendmail_required))
                        .withEnabled(true)
                .build()
        );
        builder.withPluginConfigurations(
                new MailSenderConfigurationBuilder()
                        .withMailTo("themetallists@freemail.hu")
                        .withSubject("ACRA ERROR REPORT")
                        .withReportAsFile(false)
                        .withEnabled(true)
                        .build()
        );


        ACRA.init(this, builder);
    }
}
