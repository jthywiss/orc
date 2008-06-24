package orc.orchard;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({PublicationEvent.class, TokenErrorEvent.class})
public abstract class JobEvent implements Serializable {
	public int sequence;
	public Date timestamp;
}
