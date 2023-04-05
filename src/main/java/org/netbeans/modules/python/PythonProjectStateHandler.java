package org.netbeans.modules.python;

import org.netbeans.spi.project.ProjectState;

/**
 *
 * @author albilu
 */
public class PythonProjectStateHandler {

    ProjectState state;

    public PythonProjectStateHandler(ProjectState state) {
        this.state = state;
    }

    public void mark() {
        state.markModified();

    }

}
