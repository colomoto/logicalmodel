package org.colomoto.biolqm.metadata.annotations;

import org.colomoto.biolqm.metadata.annotations.Annotation;
import org.colomoto.biolqm.metadata.annotations.Author;
import org.colomoto.biolqm.metadata.annotations.Metadata;

import org.colomoto.biolqm.metadata.constants.ModelConstants;
import org.colomoto.biolqm.metadata.constants.ListMetadata;
import org.colomoto.biolqm.metadata.constants.Index;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * Special class of annotations
 * This contains the annotations relative to the authors of the model
 *
 * @author Martin Boutroux
 */
class AuthorsAnnotation extends Annotation {
	
	// variables
	private ArrayList<Author> listOfAuthors;
	
	private Index indexOfAuthors;
	
	// constructors
	protected AuthorsAnnotation() {
		this.listOfAuthors = new ArrayList<Author>();
		
		this.indexOfAuthors = null;
	}
		
	// getters
	protected ArrayList<Author> getListOfAuthors() {
		return this.listOfAuthors;
	}
	
	// functions
	private void removeIndexParent(Index index) {
		Index indexParent = index.getIndexOfParent();
		indexParent.setIndexOfChildren(index);
	}
	
	private void removeIndexChildren(ModelConstants modelConstants, Index index) {
		for (Index indexChild: index.getIndexOfChildren()) {
			if (indexChild.getIndexOfChildren().size() != 0) {
				removeIndexChildren(modelConstants, indexChild);
			}
			modelConstants.getListMetadata().remove(indexChild);
		}
	}

	@Override
	protected void addAnnotation(ModelConstants modelConstants, String component, String termDesired, String[] contentAnnotation) {

		Author author = new Author(contentAnnotation[0], contentAnnotation[1], contentAnnotation[2], contentAnnotation[3], contentAnnotation[4]);
		this.listOfAuthors.add(author);
	}

	@Override
	protected boolean removeAnnotation(ModelConstants modelConstants, String[] contentAnnotation) {

		Author author = new Author(contentAnnotation[0], contentAnnotation[1], contentAnnotation[2], contentAnnotation[3], contentAnnotation[4]);
		if (!this.listOfAuthors.contains(author)) {
			System.out.println("This value has not been defined yet for this qualifier." + "\n");
		}
		else {		
			this.listOfAuthors.remove(author);
		}
		
		if (this.listOfAuthors.size() == 0) {
			this.removeIndexParent(indexOfAuthors);
			this.removeIndexChildren(modelConstants, indexOfAuthors);
					
			modelConstants.getListMetadata().remove(indexOfAuthors);
			
			return true;
		}
		return false;
	}

	@Override
	protected String getValue() {
		
		String chaine = "";
		if (this.indexOfAuthors != null) {
			chaine += " (nested)";
		}
		chaine += ":\n";
		
		chaine += "\tAuthors :\n";
		for (Author author : this.listOfAuthors) {
			chaine += "\t\t" + author.getName() + ", " + author.getSurname() + ", " + author.getEmail() + ", " + author.getOrganisation() + ", " + author.getOrcid() + "\n";
		}
		
		return chaine;
	}
	
	@Override
	protected Index getIndex(ModelConstants modelConstants, Index indexParent) {

		Index existingIndex;
		
		if (this.indexOfAuthors != null) {
			existingIndex = this.indexOfAuthors;
		}
		else {
			existingIndex = new Index(indexParent, modelConstants.getIncrement());
			
			indexParent.setIndexOfChildren(existingIndex);
			
			this.indexOfAuthors = existingIndex;
			modelConstants.getListMetadata().put(existingIndex, new Metadata(modelConstants, "nested"));
		}
		
		return existingIndex;
	}
}