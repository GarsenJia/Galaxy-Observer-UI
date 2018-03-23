package application.compress;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Converter
public class StringArrayToListConverter implements AttributeConverter<String[], List<String>> {
	
	@Override
	public List<String> convertToDatabaseColumn(final String[] attribute) {
		if (attribute == null || attribute.length <= 0) {
			return new ArrayList<>();
		}
		return Arrays.asList(attribute);
	}
	
	@Override
	public String[] convertToEntityAttribute(final List<String> dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return new String[0];
		}
		
		return dbData.toArray(new String[0]);
	}
	
}
