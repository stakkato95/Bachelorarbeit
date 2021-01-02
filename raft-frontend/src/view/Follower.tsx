import React from 'react';

import { createStyles, Theme, makeStyles } from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import Avatar from '@material-ui/core/Avatar';
import ImageIcon from '@material-ui/icons/Image';
import WorkIcon from '@material-ui/icons/Work';
import BeachAccessIcon from '@material-ui/icons/BeachAccess';

import Collapse from '@material-ui/core/Collapse';
import ExpandLess from '@material-ui/icons/ExpandLess';
import ExpandMore from '@material-ui/icons/ExpandMore';

import TableContainer from '@material-ui/core/TableContainer';
import Paper from '@material-ui/core/Paper';

import Log from './Log';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            width: '100%',
            maxWidth: 360,
            backgroundColor: theme.palette.background.paper,
        },
        nested: {
            paddingLeft: theme.spacing(4),
        },
        table: {
        },
    }),
);

export default function Follower(props: any) {
    const { follower } = props;

    const classes = useStyles();
    const [openLog, setOpenLog] = React.useState(false);
    const handleClickLog = () => setOpenLog(!openLog);

    let lastApplied = follower.lastApplied === undefined ? "(no values commited yet)" : follower.lastApplied
    let timeout = follower.heartBeatTimeout.length + " " + follower.heartBeatTimeout.unit

    return (<>
        <List className={classes.root}>
            <ListItem>
                <ListItemAvatar>
                    <Avatar>
                        <ImageIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary={follower.nodeId} secondary="id" />
            </ListItem>
            <ListItem>
                <ListItemAvatar>
                    <Avatar>
                        <WorkIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary={lastApplied} secondary="Last applied index" />
            </ListItem>
            <ListItem>
                <ListItemAvatar>
                    <Avatar>
                        <WorkIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary={timeout} secondary="Leader election timeout" />
            </ListItem>
            {/*  */}
            <ListItem button onClick={handleClickLog}>
                <ListItemAvatar>
                    <Avatar>
                        <BeachAccessIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary="Log" />
                {openLog ? <ExpandLess /> : <ExpandMore />}
            </ListItem>
            <Collapse in={openLog} timeout="auto" unmountOnExit>
                <TableContainer component={Paper}>
                    <Log log={follower.log}/>
                </TableContainer>
            </Collapse>
        </List>
    </>);
}