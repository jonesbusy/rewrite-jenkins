/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.jenkins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.AddOrUpdateChild;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Disables local file resolution for parent POM, as recommended by the
 * <a href="https://www.jenkins.io/doc/developer/plugin-development/updating-parent/">plugin development guide</a>.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class DisableLocalResolutionForParentPom extends Recipe {
    @Override
    public String getDisplayName() {
        return "Disables local file resolution for parent POM";
    }

    @Override
    public String getDescription() {
        return "Explicitly sets `<relativePath/>` to disable file resolution, as recommended in the " +
                "[plugin development guide](https://www.jenkins.io/doc/developer/plugin-development/updating-parent/).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isParentTag()) {
                    Tag relativePathTag = Tag.build("<relativePath/>");
                    List<Content> contents = new ArrayList<>(tag.getContent());
                    // Skip if relativePath is already present
                    if (contents.stream().anyMatch(content -> content instanceof Tag && ((Xml.Tag)content).getName().equals("relativePath"))) {
                        return tag;
                    }
                    Optional<Tag> maybeChild = tag.getChild("artifactId");
                    if (!maybeChild.isPresent()) {
                        return tag;

                    }
                    relativePathTag = relativePathTag.withPrefix(maybeChild.get().getPrefix());

                    contents.add(relativePathTag);
                    return tag.withContent(contents);
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
