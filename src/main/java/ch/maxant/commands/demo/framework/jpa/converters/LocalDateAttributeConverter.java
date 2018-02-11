package ch.maxant.commands.demo.framework.jpa.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.LocalDate;
import java.sql.Date;

//http://www.thoughts-on-java.org/persist-localdate-localdatetime-jpa/
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

    @Override
    public Date convertToDatabaseColumn(LocalDate locDate) {
    	return (locDate == null ? null : Date.valueOf(locDate));
    }

    @Override
    public LocalDate convertToEntityAttribute(Date sqlDate) {
    	return (sqlDate == null ? null : sqlDate.toLocalDate());
    }
}