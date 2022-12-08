package excelCconverter;

import lombok.Data;

@Data
public class Object {
    private final int epochDay;
    private final String currencyCode;
    private final double rate;
}
