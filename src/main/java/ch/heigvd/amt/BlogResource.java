package ch.heigvd.amt;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import java.io.StringWriter;

@Path("/")
public class BlogResource {

    @Inject
    BlogService blogService;

    @Inject
    Template index;

    @Blocking
    @GET()
    @Path("/sitemap.xml")
    @Produces(MediaType.APPLICATION_XML)
    public String sitemap() {
        try {
            // Init marshaller
            JAXBContext jaxbContext = JAXBContext.newInstance(Urlset.class);
            Marshaller marshaller = jaxbContext.createMarshaller();

            // Add URls to sitemap
            Urlset urlSet = new Urlset();
            
            // Add root url
            TUrl rootUrl = new TUrl();
            rootUrl.setLoc("http://localhost:8080/");
            urlSet.getUrl().add(rootUrl);

            // Add posts urls
            for (Post post : blogService.findAllPosts()) {
                // Add url with location and lastmod
                TUrl url = new TUrl();
                url.setLoc("http://localhost:8080/posts/" + post.getSlug() + ".html");
                url.setLastmod(post.getDate().toString());
                // Add in set
                urlSet.getUrl().add(url);
            }

            // Format to XML
            StringWriter writer = new StringWriter();
            marshaller.marshal(urlSet, writer);
            return writer.toString();
        } catch (Exception e) {
            return "<error>Erreur lors de la génération du sitemap</error>";
        }
    }

    @Blocking
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return index.data("posts", blogService.findAllPosts());
    }

    @Inject
    Template create;

    @Blocking
    @Path("/posts/create.html")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance create() {
        return create.instance();
    }

    @Inject
    Template created;

    @Blocking
    @Path("/posts/")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance save(@FormParam("author") String author, @FormParam("title") String title, @FormParam("content") String content) {
        var post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setSlug(title.toLowerCase().replace(" ", "-"));
        post.setDate(java.time.Instant.now());
        post.setAuthor(blogService.getOrCreateAuthor(author));
        blogService.createPost(post);
        return created.instance();
    }

    @Inject
    Template view;

    @Blocking
    @Path("/posts/{slug}.html")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance view(@PathParam("slug") String slug) {
        return view.data("post", blogService.findPostBySlug(slug));
    }

}
