package excelCconverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Bishkek");
    private static final Set<String> CURRENCIES = Set.of(
            "USD",
            "EUR",
            "GBP",
            "DKK",
            "INR",
            "CAD",
            "CNY",
            "KRW",
            "NOK",
            "XDR",
            "SEK",
            "CHF",
            "JPY",
            "AMD",
            "RUB",
            "BYN",
            "KZT",
            "MDL",
            "TJS",
            "UZS",
            "UAH",
            "KWD",
            "HUF",
            "CZK",
            "NZD",
            "PKR",
            "AUD",
            "TRY",
            "AZN",
            "SGD",
            "AFN",
            "BGN",
            "BRL",
            "GEL",
            "AED",
            "IRR",
            "MYR",
            "MNT",
            "TWD",
            "TMT",
            "PLN",
            "SAR");

    private static final String SQL_QUERY = "INSERT INTO nbkr_exchange_rates (epoch_day, currency_code, rate) VALUES\n" +
            "%s;";

    private static final String SQL_VALUE = "(%d, '%s', %f)";

    public static void main(String[] args) throws IOException, InvalidFormatException {
        List<Object> objects1 = getObjects("/Users/esemenovv/Downloads/allvalsrus.xls");
        List<Object> objects = getObjects("/Users/esemenovv/Downloads/dailyrus.xls");
        //System.out.println(objects.size());

        //objects.forEach(System.out::println);
        objects1.addAll(objects);
        //System.out.println(objects1);
/*
        System.out.println(objects1.stream()
                .filter(object -> Objects.equals(object.getCurrencyCode(), "BYN") || Objects.equals(object.getCurrencyCode(), "BYR"))
                .collect(Collectors.toList()));*/
        System.out.println(objects1.size());

        String values = objects1.stream()
                .filter(object -> CURRENCIES.contains(object.getCurrencyCode()))
                .distinct()
                .sorted(Comparator.comparingInt(Object::getEpochDay))
                .map(object -> String.format(SQL_VALUE, object.getEpochDay(), object.getCurrencyCode(), object.getRate()))
                .collect(Collectors.joining(",\n"));

        String result = String.format(SQL_QUERY, values);

        //System.out.println(result);

        Path path = Paths.get("output.txt");

        try {
            Files.writeString(path, result, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    public static List<Object> getObjects(String fileName) throws IOException {
        Workbook workBook = WorkbookFactory.create(new File(fileName));


        List<Object> objects = new ArrayList<>();
        for (Sheet sheet : workBook) {
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();


            Row first1Row = sheet.getRow(firstRow);
            short firstRowFirstCellNum = first1Row.getFirstCellNum();
            short firstRowLastCellNum = first1Row.getLastCellNum();
            List<Valute> valutes = new ArrayList<>();

            for (int index = firstRowFirstCellNum + 1; index <= firstRowLastCellNum; index++) {
                String stringCellValue = first1Row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
                if (stringCellValue.length() == 0)
                    break;
                if (stringCellValue.length() == 3) {
                    valutes.add(new Valute(stringCellValue, 1));
                } else {
                    String currencyCode = StringUtils.right(stringCellValue, 3);
                    StringBuilder stringBuilder = new StringBuilder();
                    int i = 0;
                    char[] chars = stringCellValue.toCharArray();
                    while (Character.isDigit(chars[i])) {
                        stringBuilder.append(chars[i]);
                        i++;
                    }

                    valutes.add(new Valute(currencyCode, Integer.parseInt(stringBuilder.toString())));
                }
            }


            for (int index = firstRow + 1; index <= lastRow; index++) {
                Row row = sheet.getRow(index);
                short firstCellNum;
                try {
                    firstCellNum = row.getFirstCellNum();
                } catch (Exception e) {
                    break;
                }
                short lastCellNum = row.getLastCellNum();

                List<Double> dayInfo = new ArrayList<>();
                for (int innerIndex = firstCellNum; innerIndex <= lastCellNum; innerIndex++) {
                    try {
                        dayInfo.add(row
                                .getCell(innerIndex)
                                .getNumericCellValue()
                        );
                    } catch (Exception e) {
                        break;
                    }
                }

                List<Object> innerObjects = new ArrayList<>();
                Date numericCellValue;
                try {
                    numericCellValue = row
                            .getCell(0)
                            .getDateCellValue();
                } catch (Exception e) {
                    break;
                }
                int epochDay = dataConverter(numericCellValue);
                for (int i = 1; i < dayInfo.size(); i++) {
                    innerObjects.add(new Object(epochDay,
                            valutes.get(i - 1).getCurrencyCode(),
                            dayInfo.get(i) / valutes.get(i - 1).getMnojitel()));
                }

                objects.addAll(innerObjects);
            }

        }
        return objects;
    }

    private static int dataConverter(Date date) {
        return (int) date.toInstant().atZone(TIME_ZONE).getLong(ChronoField.EPOCH_DAY);
    }
}
