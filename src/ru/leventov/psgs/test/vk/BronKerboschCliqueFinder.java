/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package ru.leventov.psgs.test.vk;

import gnu.trove.function.Consumer;
import gnu.trove.set.hash.DHashSet;
import ru.leventov.psgs.DeserializationException;
import ru.leventov.psgs.Edge;
import ru.leventov.psgs.ExistingGraph;
import ru.leventov.psgs.Graph;
import ru.leventov.psgs.io.NoData;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class implements Bron-Kerbosch clique detection algorithm as it is
 * described in [Samudrala R.,Moult J.:A Graph-theoretic Algorithm for
 * comparative Modeling of Protein Structure; J.Mol. Biol. (1998); vol 279; pp.
 * 287-302]
 *
 * @author Ewgenij Proschak
 */
public class BronKerboschCliqueFinder {

    public static void main(String[] args) throws IOException, DeserializationException {
        Path pathToGraph = Paths.get(args[0]);
        int rootId = Integer.parseInt(args[1]);
        BronKerboschCliqueFinder bronKerboschCliqueFinder =
                new BronKerboschCliqueFinder(ExistingGraph.openForReading(pathToGraph), rootId);
        Collection<Set<Person>> biggestMaximalCliques = bronKerboschCliqueFinder.getBiggestMaximalCliques();
        for (Set<Person> maximalClique : biggestMaximalCliques) {
            System.out.println("Clique of size " + maximalClique.size() + ": ");
            for (Person person : maximalClique) {
                System.out.println(person.getId());
            }
            System.out.println();
        }
    }

    private final Friendship friendship;
    private final Person root;

    private Collection<Set<Person>> cliques;

    public BronKerboschCliqueFinder(Graph graph, int rootId) {
        friendship = new Friendship(graph);
        this.root = (Person) graph.getNode(rootId);
    }

    public Collection<Set<Person>> getAllMaximalCliques() {
        cliques = new ArrayList<>();
        List<Person> potential_clique = new ArrayList<>();
        final List<Person> candidates = new ArrayList<>();
        List<Person> already_found = new ArrayList<>();
        friendship.from(root).forEach(new Consumer<Edge<Person, Person, NoData>>() {
            @Override
            public void accept(Edge<Person, Person, NoData> friendship) {
                candidates.add(friendship.getTarget());
            }
        });
        candidates.add(root);
        findCliques(potential_clique, candidates, already_found);
        return cliques;
    }

    /**
     * Finds the biggest maximal cliques of the graph.
     *
     * @return Collection of cliques (each of which is represented as a Set of
     *         vertices)
     */
    public Collection<Set<Person>> getBiggestMaximalCliques() {
        // first, find all cliques
        getAllMaximalCliques();

        int maximum = 0;
        Collection<Set<Person>> biggest_cliques = new ArrayList<Set<Person>>();
        for (Set<Person> clique : cliques) {
            if (maximum < clique.size()) {
                maximum = clique.size();
            }
        }
        for (Set<Person> clique : cliques) {
            if (maximum == clique.size()) {
                biggest_cliques.add(clique);
            }
        }
        return biggest_cliques;
    }

    private void findCliques(
            List<Person> potential_clique,
            List<Person> candidates,
            List<Person> already_found) {
        List<Person> candidates_array = new ArrayList<Person>(candidates);
        if (!end(candidates, already_found)) {
            // for each candidate_node in candidates do
            for (Person candidate : candidates_array) {
                List<Person> new_candidates = new ArrayList<Person>();
                List<Person> new_already_found = new ArrayList<Person>();

                // move candidate node to potential_clique
                potential_clique.add(candidate);
                candidates.remove(candidate);

                // create new_candidates by removing nodes in candidates not
                // connected to candidate node
                for (Person new_candidate : candidates) {
                    if (friendship.from(candidate).isPresentTo(new_candidate.getId())) {
                        new_candidates.add(new_candidate);
                    } // of if
                } // of for

                // create new_already_found by removing nodes in already_found
                // not connected to candidate node
                for (Person new_found : already_found) {
                    if (friendship.from(candidate).isPresentTo(new_found.getId())) {
                        new_already_found.add(new_found);
                    } // of if
                } // of for

                // if new_candidates and new_already_found are empty
                if (new_candidates.isEmpty() && new_already_found.isEmpty()) {
                    // potential_clique is maximal_clique
                    cliques.add(new DHashSet<>(potential_clique));
                } // of if
                else {
                    // recursive call
                    findCliques(
                            potential_clique,
                            new_candidates,
                            new_already_found);
                } // of else

                // move candidate_node from potential_clique to already_found;
                already_found.add(candidate);
                potential_clique.remove(candidate);
            } // of for
        } // of if
    }

    private boolean end(List<Person> candidates, List<Person> already_found) {
        // if a node in already_found is connected to all nodes in candidates
        boolean end = false;
        int edgecounter;
        for (Person found : already_found) {
            edgecounter = 0;
            for (Person candidate : candidates) {
                if (friendship.from(found).isPresentTo(candidate.getId())) {
                    edgecounter++;
                } // of if
            } // of for
            if (edgecounter == candidates.size()) {
                end = true;
            }
        } // of for
        return end;
    }
}
