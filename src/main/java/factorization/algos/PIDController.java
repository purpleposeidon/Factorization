package factorization.algos;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.shared.NORELEASE;
import factorization.util.NumUtil;

import java.io.IOException;

public class PIDController {
    private double P, I, D;
    private double integrationMin, integrationMax;

    public PIDController(final double p, final double i, final double d) {
        P = p;
        I = i;
        D = d;
    }

    public PIDController setIntegrationFail(double fail) {
        integrationMin = -fail;
        integrationMax = +fail;
        return this;
    }

    public PIDController setIntegrationLimits(double min, double max) {
        this.integrationMin = min;
        this.integrationMax = max;
        return this;
    }

    public void adjustPID(final double p, final double i, final double d) {
        // Intended for debugging
        P = p;
        I = i;
        D = d;
    }

    public void setDt(int dt) {
        this.dt = dt;
    }

    int dt;

    public double previous_error = 0;
    public double integral = 0;

    public double tick(double setpoint, double measured_value) {
        double error = setpoint - measured_value;
        integral = integral + error * dt;
        double derivative = (error - previous_error) / dt;
        double output = P * error + I * integral + D * derivative;
        previous_error = error;
        integral = NumUtil.clip(integral, integrationMin, integrationMax);
        return output;
    }

    public void putData(DataHelper data, String prefix) throws IOException {
        // PID parameters should be standard
        //P = data.as(Share.PRIVATE, prefix + "P").putDouble(P);
        //I = data.as(Share.PRIVATE, prefix + "I").putDouble(I);
        //D = data.as(Share.PRIVATE, prefix + "D").putDouble(D);
        if (NORELEASE.off) {
            previous_error = data.as(Share.PRIVATE, prefix + "previousError").putDouble(previous_error);
            integral = data.as(Share.PRIVATE, prefix + "integral").putDouble(integral);
        }
    }
}
