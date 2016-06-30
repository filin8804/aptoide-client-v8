package cm.aptoide.pt.model.v7.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

import cm.aptoide.pt.model.v7.listapp.App;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class Feature  {

	@Getter private final String title;
	@Getter private final String thumbnailUrl;
	@Getter private final String url;
	@Getter private final Date date;
	@Getter private final List<App> apps;

	@JsonCreator
	public Feature(@JsonProperty("title") String title,
	               @JsonProperty("thumbnail") String thumbnailUrl,
	               @JsonProperty("url") String url,
	               @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC") @JsonProperty("date") Date date,
	               @JsonProperty("apps") List<App> apps) {
		this.title = title;
		this.thumbnailUrl = thumbnailUrl;
		this.url = url;
		this.date = date;
		this.apps = apps;
	}
}
