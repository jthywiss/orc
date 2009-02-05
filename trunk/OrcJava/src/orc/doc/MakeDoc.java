package orc.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import orc.runtime.ReverseListIterator;

import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * @author quark
 */
public class MakeDoc {
	private static class DocNodes {
		public String file;
		public List<DocNode> nodes;
		public DocNodes(String file, List<DocNode> nodes) {
			this.file = file;
			this.nodes = nodes;
		}
	}
	
	public static void main(String[] args) throws ParseException, IOException {
		List<DocNodes> files = new LinkedList<DocNodes>();
		for (String file : args) {
			files.add(new DocNodes(file, parseFile(file)));
		}
		System.out.println("<?xml version=\"1.0\"?>");
		System.out.println("<section><title>Reference</title>");
		int anchor;
		anchor = 0;
		for (DocNodes file : files) {
			System.out.print("<section><title>");
			System.out.print(escapeXML(file.file));
			if (!file.nodes.isEmpty()) {
				DocNode first = file.nodes.get(0);
				if (first instanceof DocParagraph) {
					System.out.print(": ");
					System.out.print(firstSentence(((DocParagraph)first).body.trim()));
				}
			}
			System.out.println("</title>");
			int depth = 0;
			for (DocNode doc : file.nodes) {
				if (doc instanceof DocParagraph) {
					String body = ((DocParagraph)doc).body.trim();
					if (!body.equals("")) {
						System.out.print("<para>");
						System.out.print(body);
						System.out.println("</para>");
					}
				} else if (doc instanceof DocTag) {
					System.out.print(((DocTag)doc).value);
				} else if (doc instanceof DocType) {
					DocType type = (DocType)doc;
					if (type.depth > depth) {
						System.out.println("<variablelist>");
						depth = type.depth;
					} else {
						while (type.depth < depth) {
							System.out.println("</listitem></varlistentry></variablelist>");
							--depth;
						}
						if (type.depth == depth) {
							System.out.println("</listitem></varlistentry>");
						}
						depth = type.depth;
					}
					System.out.print("<varlistentry><term>");
					System.out.print("<code>" + escapeXML(extractName(type.type)) + "</code>");
					System.out.print("</term><listitem>");
					System.out.print("<para>");
					System.out.print("<code>" + escapeXML(type.type) + "</code>");
					System.out.println("</para>");
				}
			}
			while (0 < depth) {
				System.out.println("</listitem></varlistentry></variablelist>");
				--depth;
			}
			System.out.println("</section>");
		}
		System.out.println("</section>");
	}
	
	public static String extractName(String type) {
		// extract everything between the declaration keyword
		// and the argument list
		return type.replaceAll("[a-z]+\\s+(.[^(]+).*", "$1")
			// drop the method receiver type
			.replaceFirst("^[^.]+\\.", "")
			// drop type parameters
			.replaceFirst("\\[[^\\]]+\\]", "");
	}
	
	public static String firstSentence(String para) {
		String[] parts = para.split("(?<=[.?!])\\s+", 2);
		return parts[0];
	}
	
	public static List<DocNode> parseFile(String file) throws ParseException, IOException {
		DocParser parser = new DocParser(
				new InputStreamReader(new FileInputStream(file)),
				file);
		Result result = parser.pContent(0);
		List<DocNode> nodes = (List<DocNode>)parser.value(result);
		
		// fill in values of @implementation tags
		DocCode lastCode = new DocCode("");
		ListIterator<DocNode> it = nodes.listIterator(nodes.size());
		while (it.hasPrevious()) {
			DocNode node = it.previous();
			if (node instanceof DocCode) {
				lastCode = (DocCode)node;
			} else if (node instanceof DocTag) {
				DocTag tag = (DocTag)node;
				if (tag.name.equals("implementation")) {
					tag.value = "<formalpara><title>Implementation</title>" +
						"<programlisting>" + escapeXML(lastCode.text.trim()) + "</programlisting>" +
						"</formalpara>";
				}
			}
		}
		return nodes;
	}

	public static String escapeXML(String text) {
		StringBuilder sb = new StringBuilder();
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			switch (c) {
			case 34:
				sb.append("&quot;");
				break;
			case 38:
				sb.append("&amp;");
				break;
			case 39:
				sb.append("&apos;");
				break;
			case 60:
				sb.append("&lt;");
				break;
			case 62:
				sb.append("&gt;");
				break;
			default:
				if (c > 0x7F) {
					sb.append("&#");
					sb.append(Integer.toString(c, 10));
					sb.append(';');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}
}
